-- 通用固定窗口限流脚本
-- KEYS[1]: 限流key
-- ARGV[1]: 窗口内允许的最大次数 limit
-- ARGV[2]: 窗口大小（秒）window
-- 返回: 1=放行, 0=拒绝

-- 1. 计数 +1
local current = redis.call('INCR', KEYS[1])

-- 2. 第一次访问时设置过期时间（保证 INCR+EXPIRE 原子，避免 key 永不过期）
if current == 1 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[2]))
end

-- 3. 超过阈值则拒绝
if current > tonumber(ARGV[1]) then
    return 0
end

return 1
