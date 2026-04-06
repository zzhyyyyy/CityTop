-- 优惠券抢购Lua脚本
-- KEYS[1]: 库存key (如: coupon:stock:1001)
-- KEYS[2]: 用户已下单集合key (如: coupon:users:1001)
-- ARGV[1]: 用户ID


-- 1. 判断库存是否充足
local stock = redis.call('GET', KEYS[1])
if not stock or tonumber(stock) <= 0 then
    return 1  -- 库存不足，返回1
end

-- 2. 判断用户是否已下单（防重复购买）
local isOrdered = redis.call('SISMEMBER', KEYS[2], ARGV[1])
if isOrdered == 1 then
    return 2  -- 用户已下单，返回2
end

-- 3. 扣减库存
redis.call('DECR', KEYS[1])

-- 4. 将用户ID存入已下单集合
redis.call('SADD', KEYS[2], ARGV[1])

return 0  -- 抢购成功，返回0