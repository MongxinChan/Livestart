-- Lua script: rollback stock and per-user purchase limit in one atomic step.
--
-- KEYS[1]: stock key
-- KEYS[2]: user limit key
-- ARGV[1]: rollback stock count
-- ARGV[2]: rollback user limit count

local rollbackStock = tonumber(ARGV[1])
local rollbackLimit = tonumber(ARGV[2])

if rollbackStock ~= nil and rollbackStock > 0 then
    redis.call('INCRBY', KEYS[1], rollbackStock)
end

local currentLimit = tonumber(redis.call('GET', KEYS[2]))
if currentLimit ~= nil and rollbackLimit ~= nil and rollbackLimit > 0 then
    local nextLimit = currentLimit - rollbackLimit
    if nextLimit > 0 then
        redis.call('SET', KEYS[2], nextLimit, 'KEEPTTL')
    else
        redis.call('DEL', KEYS[2])
    end
end

return 0
