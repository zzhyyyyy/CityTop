package com.citytop.canalsync.service;

import com.citytop.canalsync.config.CanalProperties;
import com.citytop.canalsync.domain.CacheSyncEvent;
import com.citytop.canalsync.repository.CacheSyncEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheReconciliationJobTest {
    private CacheSyncEventRepository repository;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private CacheReconciliationJob job;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        repository = mock(CacheSyncEventRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        CanalProperties properties = new CanalProperties();
        properties.getRetry().setBaseDelaySeconds(1);
        properties.getRetry().setMaxAttempts(3);
        job = new CacheReconciliationJob(repository, redisTemplate, properties);
    }

    @Test
    void shouldFinishDelayedShopCacheDeletion() {
        CacheSyncEvent event = event("event-shop", "cache:shop:1", "CACHE_INVALIDATE", 0);

        job.reconcileOne(event);

        verify(redisTemplate).delete("cache:shop:1");
        verify(repository).markSuccess("event-shop");
    }

    @Test
    void shouldRetryInsteadOfOverwritingMismatchedSeckillStock() {
        CacheSyncEvent event = event("event-stock", "seckill:stock:10", "RECONCILE_SIGNAL", 0);
        when(repository.findVoucherStock("10")).thenReturn(8);
        when(valueOperations.get("seckill:stock:10")).thenReturn("7");

        job.reconcileOne(event);

        verify(repository).markRetry(anyString(), anyInt(), any(), anyString());
        verify(valueOperations, never()).set(anyString(), anyString());
        verify(repository, never()).markSuccess("event-stock");
    }

    private static CacheSyncEvent event(String id, String cacheKey, String syncMode, int retryCount) {
        CacheSyncEvent event = new CacheSyncEvent();
        event.setEventId(id);
        event.setCacheKey(cacheKey);
        event.setSyncMode(syncMode);
        event.setRetryCount(retryCount);
        return event;
    }
}
