-- Lua 脚本: 基于 Sorted Set 的滑动窗口限流
--
-- KEYS[1]: 限流 Key（engine:ratelimit:{userId}:{uri}）
-- ARGV[1]: 当前时间戳（毫秒）
-- ARGV[2]: 窗口大小（毫秒）
-- ARGV[3]: 最大允许请求数
-- ARGV[4]: 唯一 member（时间戳:随机后缀，保证 ZSet 成员唯一性）
--
-- 返回值:
--   0 = 放行
--   1 = 限流（超出阈值）

local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local maxPermits = tonumber(ARGV[3])
local member = ARGV[4]

-- 1. 清除窗口外的过期记录
local windowStart = now - windowMs
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

-- 2. 统计当前窗口内的请求数
local currentCount = redis.call('ZCARD', key)

-- 3. 判断是否超限
if currentCount >= maxPermits then
    return 1
end

-- 4. 未超限，添加本次请求
redis.call('ZADD', key, now, member)

-- 5. 设置 Key 过期时间（窗口大小的 2 倍，防止 Key 无限膨胀）
local expireSeconds = math.ceil(windowMs * 2 / 1000)
if expireSeconds < 2 then
    expireSeconds = 2
end
redis.call('EXPIRE', key, expireSeconds)

return 0
