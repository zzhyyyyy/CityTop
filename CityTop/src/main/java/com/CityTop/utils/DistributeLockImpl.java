package com.CityTop.utils;

import cn.hutool.core.lang.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
@Slf4j
public class DistributeLockImpl implements DistributeLock{
    private final StringRedisTemplate stringRedisTemplate;
    private final String name;
    private static final String ID_PREFIX = UUID.randomUUID().toString(true)+"-";
    public DistributeLockImpl(StringRedisTemplate stringRedisTemplate,String name) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.name = name;
    }
    private static final  String KEY_PREFIX = "lock:";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }
    @Override
    public boolean tryLock(long timeoutSec) {
        long id = Thread.currentThread().getId();
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, ID_PREFIX + id, timeoutSec, TimeUnit.SECONDS));
    }

    @Override
    public void unlock() {
        log.info("执行lua脚本删锁");
//        if((ID_PREFIX+Thread.currentThread().getId()).equals(stringRedisTemplate.opsForValue().get(KEY_PREFIX + name)))
//            stringRedisTemplate.delete("lock");
        //采用lua脚本
        stringRedisTemplate.execute(UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX + Thread.currentThread().getId());
    }
}
