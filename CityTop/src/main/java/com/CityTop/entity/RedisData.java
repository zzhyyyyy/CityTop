package com.CityTop.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private Object data;        // 实际数据
    private LocalDateTime expireTime;  // 逻辑过期时间
}