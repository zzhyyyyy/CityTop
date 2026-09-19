package com.citytop.canalsync.service;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.citytop.canalsync.config.CanalProperties;
import com.citytop.canalsync.domain.CacheSyncEvent;
import com.citytop.canalsync.repository.CacheSyncEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanalEventProcessorTest {
    private CacheSyncEventRepository repository;
    private StringRedisTemplate redisTemplate;
    private CanalEventProcessor processor;

    @BeforeEach
    void setUp() {
        repository = mock(CacheSyncEventRepository.class);
        redisTemplate = mock(StringRedisTemplate.class);
        CanalProperties properties = new CanalProperties();
        properties.setVerifyDelayMillis(100);
        processor = new CanalEventProcessor(repository, redisTemplate, properties);
        when(repository.insertIfAbsent(any(CacheSyncEvent.class))).thenReturn(true);
    }

    @Test
    void shouldDeleteShopCacheAndScheduleDelayedVerification() throws Exception {
        processor.process(Collections.singletonList(entry(
                "tb_shop",
                CanalEntry.EventType.UPDATE,
                column("id", "7", true),
                column("name", "新店名", false))));

        ArgumentCaptor<CacheSyncEvent> captor = ArgumentCaptor.forClass(CacheSyncEvent.class);
        verify(repository).insertIfAbsent(captor.capture());
        verify(redisTemplate).delete("cache:shop:7");
        verify(repository).markWaitingVerification(anyString(), any());

        CacheSyncEvent event = captor.getValue();
        assertEquals("CACHE_INVALIDATE", event.getSyncMode());
        assertEquals("cache:shop:7", event.getCacheKey());
        assertEquals("id=7", event.getRowKey());
        assertNotNull(event.getNextRetryAt());
    }

    @Test
    void shouldOnlyCreateReconciliationSignalForSeckillStock() throws Exception {
        processor.process(Collections.singletonList(entry(
                "tb_seckill_voucher",
                CanalEntry.EventType.UPDATE,
                column("voucher_id", "10", true),
                column("stock", "8", false))));

        ArgumentCaptor<CacheSyncEvent> captor = ArgumentCaptor.forClass(CacheSyncEvent.class);
        verify(repository).insertIfAbsent(captor.capture());
        verify(redisTemplate, never()).delete(anyString());
        verify(repository).markWaitingVerification(anyString(), any());

        assertEquals("RECONCILE_SIGNAL", captor.getValue().getSyncMode());
        assertEquals("seckill:stock:10", captor.getValue().getCacheKey());
    }

    @Test
    void shouldIgnoreReplayedEventAfterAuditDeduplication() throws Exception {
        when(repository.insertIfAbsent(any(CacheSyncEvent.class))).thenReturn(false);

        processor.process(Collections.singletonList(entry(
                "tb_shop",
                CanalEntry.EventType.UPDATE,
                column("id", "7", true))));

        verify(redisTemplate, never()).delete(anyString());
        verify(repository, never()).markWaitingVerification(anyString(), any());
    }

    private static CanalEntry.Entry entry(String table,
                                          CanalEntry.EventType eventType,
                                          CanalEntry.Column... columns) {
        CanalEntry.RowData rowData = CanalEntry.RowData.newBuilder()
                .addAllAfterColumns(java.util.Arrays.asList(columns))
                .build();
        CanalEntry.RowChange rowChange = CanalEntry.RowChange.newBuilder()
                .setEventType(eventType)
                .addRowDatas(rowData)
                .build();
        CanalEntry.Header header = CanalEntry.Header.newBuilder()
                .setLogfileName("binlog.000001")
                .setLogfileOffset(123L)
                .setSchemaName("sql_store")
                .setTableName(table)
                .build();
        return CanalEntry.Entry.newBuilder()
                .setEntryType(CanalEntry.EntryType.ROWDATA)
                .setHeader(header)
                .setStoreValue(rowChange.toByteString())
                .build();
    }

    private static CanalEntry.Column column(String name, String value, boolean key) {
        return CanalEntry.Column.newBuilder()
                .setName(name)
                .setValue(value)
                .setIsKey(key)
                .build();
    }
}
