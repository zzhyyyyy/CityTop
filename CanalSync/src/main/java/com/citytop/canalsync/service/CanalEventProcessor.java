package com.citytop.canalsync.service;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.citytop.canalsync.config.CanalProperties;
import com.citytop.canalsync.domain.CacheSyncEvent;
import com.citytop.canalsync.repository.CacheSyncEventRepository;
import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CanalEventProcessor {
    static final String CACHE_INVALIDATE = "CACHE_INVALIDATE";
    static final String RECONCILE_SIGNAL = "RECONCILE_SIGNAL";
    static final String SHOP_CACHE_PREFIX = "cache:shop:";
    static final String SECKILL_STOCK_PREFIX = "seckill:stock:";

    private static final Logger log = LoggerFactory.getLogger(CanalEventProcessor.class);

    private final CacheSyncEventRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final CanalProperties properties;

    public CanalEventProcessor(CacheSyncEventRepository repository,
                               StringRedisTemplate redisTemplate,
                               CanalProperties properties) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void process(List<CanalEntry.Entry> entries) throws InvalidProtocolBufferException {
        for (CanalEntry.Entry entry : entries) {
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            CanalEntry.EventType eventType = rowChange.getEventType();
            if (eventType != CanalEntry.EventType.INSERT
                    && eventType != CanalEntry.EventType.UPDATE
                    && eventType != CanalEntry.EventType.DELETE) {
                continue;
            }

            List<CanalEntry.RowData> rows = rowChange.getRowDatasList();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                processRow(entry, eventType, rows.get(rowIndex), rowIndex);
            }
        }
    }

    private void processRow(CanalEntry.Entry entry,
                            CanalEntry.EventType eventType,
                            CanalEntry.RowData rowData,
                            int rowIndex) {
        String tableName = entry.getHeader().getTableName();
        List<CanalEntry.Column> columns = eventType == CanalEntry.EventType.DELETE
                ? rowData.getBeforeColumnsList()
                : rowData.getAfterColumnsList();
        CacheSyncEvent event = buildEvent(entry, eventType, columns, rowIndex);
        if (event == null || !repository.insertIfAbsent(event)) {
            return;
        }

        if (CACHE_INVALIDATE.equals(event.getSyncMode())) {
            invalidateShopCache(event);
            return;
        }

        // 秒杀库存由 Redis 预扣、MySQL 最终落库。这里仅安排延迟对账，
        // 不能看到 MySQL 变化就直接覆盖 Redis，否则会抹掉尚未落库的在途订单。
        repository.markWaitingVerification(
                event.getEventId(),
                LocalDateTime.now().plusNanos(properties.getVerifyDelayMillis() * 1_000_000L));
        log.info("已记录秒杀库存对账信号，table={}, rowKey={}, cacheKey={}",
                tableName, event.getRowKey(), event.getCacheKey());
    }

    private void invalidateShopCache(CacheSyncEvent event) {
        try {
            redisTemplate.delete(event.getCacheKey());
            // 延迟再次删除用于覆盖“旧值查询已开始、Canal 先删、旧值后回填”的竞态。
            repository.markWaitingVerification(
                    event.getEventId(),
                    LocalDateTime.now().plusNanos(properties.getVerifyDelayMillis() * 1_000_000L));
            log.info("已删除商铺缓存并安排延迟复核，cacheKey={}", event.getCacheKey());
        } catch (RuntimeException ex) {
            repository.markRetry(
                    event.getEventId(),
                    1,
                    LocalDateTime.now().plusSeconds(properties.getRetry().getBaseDelaySeconds()),
                    ex.getMessage());
            log.warn("删除商铺缓存失败，已进入补偿队列，cacheKey={}", event.getCacheKey(), ex);
        }
    }

    CacheSyncEvent buildEvent(CanalEntry.Entry entry,
                              CanalEntry.EventType eventType,
                              List<CanalEntry.Column> columns,
                              int rowIndex) {
        String tableName = entry.getHeader().getTableName();
        String rowKey = primaryKey(columns);
        String cacheKey;
        String syncMode;

        if ("tb_shop".equals(tableName)) {
            String shopId = valueOf(columns, "id");
            if (shopId == null) {
                return null;
            }
            cacheKey = SHOP_CACHE_PREFIX + shopId;
            syncMode = CACHE_INVALIDATE;
        } else if ("tb_seckill_voucher".equals(tableName) || "tb_voucher_order".equals(tableName)) {
            String voucherId = valueOf(columns, "voucher_id");
            if (voucherId == null) {
                return null;
            }
            cacheKey = SECKILL_STOCK_PREFIX + voucherId;
            syncMode = RECONCILE_SIGNAL;
        } else {
            return null;
        }

        String rawEventId = entry.getHeader().getLogfileName() + ":"
                + entry.getHeader().getLogfileOffset() + ":"
                + entry.getHeader().getSchemaName() + ":"
                + tableName + ":" + eventType.name() + ":" + rowIndex + ":" + rowKey;

        CacheSyncEvent event = new CacheSyncEvent();
        event.setEventId(sha256(rawEventId));
        event.setSourceLogFile(entry.getHeader().getLogfileName());
        event.setSourceLogOffset(entry.getHeader().getLogfileOffset());
        event.setSchemaName(entry.getHeader().getSchemaName());
        event.setTableName(tableName);
        event.setRowKey(rowKey);
        event.setOperation(eventType.name());
        event.setCacheKey(cacheKey);
        event.setSyncMode(syncMode);
        event.setStatus("PENDING");
        event.setRetryCount(0);
        // 审计记录先于 Redis 操作落库。若进程在两步之间崩溃，扫描任务会接管该事件，
        // 避免事件因 Canal 重放时被幂等键拦截而永久停留在 PENDING。
        event.setNextRetryAt(LocalDateTime.now()
                .plusSeconds(properties.getRetry().getBaseDelaySeconds()));
        return event;
    }

    private static String primaryKey(List<CanalEntry.Column> columns) {
        StringBuilder result = new StringBuilder();
        for (CanalEntry.Column column : columns) {
            if (!column.getIsKey()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(',');
            }
            result.append(column.getName()).append('=').append(column.getValue());
        }
        return result.length() == 0 ? "unknown" : result.toString();
    }

    private static String valueOf(List<CanalEntry.Column> columns, String columnName) {
        for (CanalEntry.Column column : columns) {
            if (columnName.equalsIgnoreCase(column.getName())) {
                return column.getIsNull() ? null : column.getValue();
            }
        }
        return null;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                hex.append(String.format("%02x", item & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", ex);
        }
    }
}
