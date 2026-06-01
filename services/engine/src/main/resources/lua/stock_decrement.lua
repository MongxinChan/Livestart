-- Lua 脚本: 原子扣减票种库存 + 校验个人购票限额
--
-- KEYS[1]: 票种库存 Key（String，存 remaining_stock 数量）
-- KEYS[2]: 用户对本次演出的购票次数 Key（String）
-- ARGV[1]: 本次购买数量
-- ARGV[2]: 单人最大购票上限（来自 event_config.max_tickets_per_user）
-- ARGV[3]: Key 过期时间（秒），通常设置为演出结束时间 + 一定冗余
--
-- 返回值（Long，高位 + 低位编码）：
--   0 = 成功
--   1 = 库存不足
--   2 = 超出个人购票限额

local function combineResult(errorCode, currentCount)
    -- 使用 14 位存放 currentCount（最大 9999 张）
    local SECOND_FIELD_BITS = 14
    return errorCode * (2 ^ SECOND_FIELD_BITS) + currentCount
end

-- 1. 获取当前剩余库存
local stock = tonumber(redis.call('GET', KEYS[1]))

-- 库存 Key 不存在或库存不足
if stock == nil or stock <= 0 then
    return combineResult(1, 0)
end

-- 本次购买数量
local buyCount = tonumber(ARGV[1])

-- 库存不足
if stock < buyCount then
    return combineResult(1, 0)
end

-- 2. 获取用户当前已购数量
local userBoughtCount = tonumber(redis.call('GET', KEYS[2]))
if userBoughtCount == nil then
    userBoughtCount = 0
end

-- 3. 校验个人限额
local maxLimit = tonumber(ARGV[2])
if (userBoughtCount + buyCount) > maxLimit then
    return combineResult(2, userBoughtCount)
end

-- 4. 原子扣减库存
redis.call('DECRBY', KEYS[1], buyCount)

-- 5. 增加用户已购数量
if userBoughtCount == 0 then
    -- 首次购买，设置 Key 并添加过期时间
    redis.call('SET', KEYS[2], buyCount)
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]))
else
    redis.call('INCRBY', KEYS[2], buyCount)
end

return combineResult(0, userBoughtCount + buyCount)
