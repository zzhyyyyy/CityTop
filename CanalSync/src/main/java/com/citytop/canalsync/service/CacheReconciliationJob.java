package com.citytop.canalsync.service;

import com.citytop.canalsync.config.CanalProperties;
import com.citytop.canalsync.domain.CacheSyncEvent;
import com.citytop.canalsync.repository.CacheSyncEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class CacheReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(CacheReconciliationJob.class);

    private final CacheSyncEventRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final CanalProperties properties;

    public CacheReconciliationJob(CacheSyncEventRepository repository,
                                  StringRedisTemplate redisTemplate,
                                  CanalProperties properties) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${citytop.canal.retry.fixed-delay-millis:2000}")
    public void reconcile() {
        try {
            List<CacheSyncEvent> events = repository.findDue(properties.getRetry().getBatchSize());
            for (CacheSyncEvent event : events) {
                reconcileOne(event);
            }
        } catch (RuntimeException ex) {
            // 数据库整体不可用时无法更新单条事件状态，只记录日志等待下一轮。
            log.error("Canal 补偿扫描失败，将在下一轮继续", ex);
        }
    }

    void reconcileOne(CacheSyncEvent event) {
        try {
            if (CanalEventProcessor.CACHE_INVALIDATE.equals(event.getSyncMode())) {
                redisTemplate.delete(event.getCacheKey());
                repository.markSuccess(event.getEventId());
                log.info("商铺缓存延迟复核完成，cacheKey={}", event.getCacheKey());
                return;
            }

            verifySeckillStock(event);
        } catch (RuntimeException ex) {
            scheduleNextAttempt(event, ex.getMessage());
        }
    }

    private void verifySeckillStock(CacheSyncEvent event) {
        String voucherId = event.getCacheKey().substring(CanalEventProcessor.SECKILL_STOCK_PREFIX.length());
        Integer mysqlStock = repository.findVoucherStock(voucherId);
        String redisValue = redisTemplate.opsForValue().get(event.getCacheKey());
        if (mysqlStock == null) {
            throw new IllegalStateException("MySQL 中不存在秒杀券 " + voucherId);
        }
        if (redisValue == null) {
            throw new IllegalStateException("Redis 秒杀库存不存在，voucherId=" + voucherId);
        }

        int redisStock;
        try {
            redisStock = Integer.parseInt(redisValue);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Redis 秒杀库存不是整数，voucherId=" + voucherId, ex);
        }

        if (mysqlStock.intValue() != redisStock) {
            throw new IllegalStateException("库存尚未一致，voucherId=" + voucherId
                    + ", mysql=" + mysqlStock + ", redis=" + redisStock);
        }

        repository.markSuccess(event.getEventId());
        log.info("秒杀库存对账通过，voucherId={}, stock={}", voucherId, mysqlStock);
    }

    private void scheduleNextAttempt(CacheSyncEvent event, String error) {
        int nextCount = event.getRetryCount() + 1;
        if (nextCount >= properties.getRetry().getMaxAttempts()) {
            repository.markManualRequired(event.getEventId(), nextCount, error);
            log.error("Canal 补偿达到最大次数，需要人工处理，eventId={}, error={}",
                    event.getEventId(), error);
            return;
        }

        long delaySeconds = backoffSeconds(nextCount);
        repository.markRetry(
                event.getEventId(),
                nextCount,
                LocalDateTime.now().plusSeconds(delaySeconds),
                error);
        log.warn("Canal 补偿未通过，稍后重试，eventId={}, retryCount={}, error={}",
                event.getEventId(), nextCount, error);
    }

    private long backoffSeconds(int retryCount) {
        int exponent = Math.min(Math.max(retryCount - 1, 0), 10);
        return ((long) properties.getRetry().getBaseDelaySeconds()) << exponent;
    }
}
