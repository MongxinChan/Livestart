-- KEYS[1]: stock key
-- KEYS[2]: user limit key
-- KEYS[3]: compensation idempotency marker
-- ARGV[1]: stock count
-- ARGV[2]: user limit count
-- ARGV[3]: marker TTL seconds

if redis.call('SETNX', KEYS[3], '1') == 0 then
    return 0
end
redis.call('EXPIRE', KEYS[3], tonumber(ARGV[3]))

local restoreStock = tonumber(ARGV[1])
local restoreLimit = tonumber(ARGV[2])
-- 缓存不存在时不要用本次归还量创建库存。保持缺失状态，后续按数据库库存完整预热。
if restoreStock ~= nil and restoreStock > 0 and redis.call('EXISTS', KEYS[1]) == 1 then
    redis.call('INCRBY', KEYS[1], restoreStock)
end

local currentLimit = tonumber(redis.call('GET', KEYS[2]))
if currentLimit ~= nil and restoreLimit ~= nil and restoreLimit > 0 then
    local nextLimit = currentLimit - restoreLimit
    if nextLimit > 0 then
        redis.call('SET', KEYS[2], nextLimit, 'KEEPTTL')
    else
        redis.call('DEL', KEYS[2])
    end
end
return 1
