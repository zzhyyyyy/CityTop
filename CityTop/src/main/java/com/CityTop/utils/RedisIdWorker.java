package com.CityTop.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
public class RedisIdWorker {
    private static final long BEGIN_TIMESTAMP = 1640995200L;
    private static final long COUNT_BITS = 32;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    public long nextId(String keyPrefix) {
        //生成时间戳
        long nowSecond = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        String data = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        //生成序列号"icr:" + keyPrefix + ":" + data
        long count = stringRedisTemplate.opsForValue().increment("icr:" + keyPrefix + ":" + data);
        return nowSecond - BEGIN_TIMESTAMP << COUNT_BITS | count;
    }

}
