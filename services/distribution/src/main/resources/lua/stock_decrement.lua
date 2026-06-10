-- Lua script: atomically decrement stock and increase per-user limit counter.
--
-- KEYS[1]: stock key
-- KEYS[2]: user limit key
-- ARGV[1]: decrement count
-- ARGV[2]: max limit
-- ARGV[3]: limit key expire seconds

local function combineResult(errorCode, currentCount)
    local secondFieldBits = 14
    return errorCode * (2 ^ secondFieldBits) + currentCount
end

local stock = tonumber(redis.call('GET', KEYS[1]))
if stock == nil or stock <= 0 then
    return combineResult(1, 0)
end

local buyCount = tonumber(ARGV[1])
if stock < buyCount then
    return combineResult(1, 0)
end

local userBoughtCount = tonumber(redis.call('GET', KEYS[2]))
if userBoughtCount == nil then
    userBoughtCount = 0
end

local maxLimit = tonumber(ARGV[2])
if (userBoughtCount + buyCount) > maxLimit then
    return combineResult(2, userBoughtCount)
end

redis.call('DECRBY', KEYS[1], buyCount)
if userBoughtCount == 0 then
    redis.call('SET', KEYS[2], buyCount)
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]))
else
    redis.call('INCRBY', KEYS[2], buyCount)
end

return combineResult(0, userBoughtCount + buyCount)
