-- 秒杀失败补偿脚本
-- KEYS[1]: 库存 key
-- KEYS[2]: 已下单用户集合 key
-- ARGV[1]: 用户 ID
-- 仅在当前用户仍持有 Redis 预扣标记时恢复一次库存，避免重复补偿。

local removed = redis.call('SREM', KEYS[2], ARGV[1])
if removed == 1 then
    redis.call('INCR', KEYS[1])
    return 1
end

return 0
