
local voucherId = ARGV[1]
local userId = ARGV[2]
local requestId = ARGV[3]
local streamKey = ARGV[4]

local stockKey = "seckill:stock:" .. voucherId
local orderKey = "seckill:order:" .. voucherId

if redis.call("SISMEMBER", orderKey, userId) == 1 then
    return 2
end

local stock = redis.call("GET", stockKey)
if not stock or tonumber(stock) <= 0 then
    return 1
end

redis.call("DECR", stockKey)

redis.call("SADD", orderKey, userId)

redis.call(
    "XADD",
    streamKey,
    "*",
    "userId", userId,
    "voucherId", voucherId,
    "requestId", requestId
)

return 0
