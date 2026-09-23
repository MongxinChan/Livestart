local stored_code = redis.call('GET', KEYS[1])
if not stored_code then
    return 0
end

if stored_code == ARGV[1] then
    redis.call('DEL', KEYS[1], KEYS[2])
    return 1
end

local attempts = redis.call('INCR', KEYS[2])
local ttl = redis.call('TTL', KEYS[2])
if attempts == 1 or ttl < 0 then
    redis.call('EXPIRE', KEYS[2], ARGV[3])
end
if attempts >= tonumber(ARGV[2]) then
    redis.call('DEL', KEYS[1], KEYS[2])
    return -2
end
return -1
