package com.citytop.canalsync.repository;

import com.citytop.canalsync.domain.CacheSyncEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class CacheSyncEventRepository {
    private final JdbcTemplate jdbcTemplate;

    public CacheSyncEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 先把 binlog 事件持久化，再操作 Redis。Canal 批次重放时依赖主键实现幂等。
     */
    public boolean insertIfAbsent(CacheSyncEvent event) {
        String sql = "INSERT IGNORE INTO cache_sync_event " +
                "(event_id, source_log_file, source_log_offset, schema_name, table_name, row_key, " +
                "operation, cache_key, sync_mode, status, retry_count, next_retry_at, last_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int changed = jdbcTemplate.update(sql,
                event.getEventId(), event.getSourceLogFile(), event.getSourceLogOffset(),
                event.getSchemaName(), event.getTableName(), event.getRowKey(),
                event.getOperation(), event.getCacheKey(), event.getSyncMode(), event.getStatus(),
                event.getRetryCount(), toTimestamp(event.getNextRetryAt()), event.getLastError());
        return changed == 1;
    }

    public void markWaitingVerification(String eventId, LocalDateTime nextRetryAt) {
        jdbcTemplate.update(
                "UPDATE cache_sync_event SET status='WAIT_VERIFY', next_retry_at=?, last_error=NULL WHERE event_id=?",
                Timestamp.valueOf(nextRetryAt), eventId);
    }

    public void markSuccess(String eventId) {
        jdbcTemplate.update(
                "UPDATE cache_sync_event SET status='SUCCESS', next_retry_at=NULL, last_error=NULL WHERE event_id=?",
                eventId);
    }

    public void markRetry(String eventId, int retryCount, LocalDateTime nextRetryAt, String error) {
        jdbcTemplate.update(
                "UPDATE cache_sync_event SET status='RETRY', retry_count=?, next_retry_at=?, last_error=? WHERE event_id=?",
                retryCount, Timestamp.valueOf(nextRetryAt), truncate(error), eventId);
    }

    public void markManualRequired(String eventId, int retryCount, String error) {
        jdbcTemplate.update(
                "UPDATE cache_sync_event SET status='MANUAL_REQUIRED', retry_count=?, next_retry_at=NULL, last_error=? WHERE event_id=?",
                retryCount, truncate(error), eventId);
    }

    public List<CacheSyncEvent> findDue(int limit) {
        return jdbcTemplate.query(
                "SELECT event_id, source_log_file, source_log_offset, schema_name, table_name, row_key, " +
                        "operation, cache_key, sync_mode, status, retry_count, next_retry_at, last_error " +
                        "FROM cache_sync_event WHERE status IN ('PENDING','WAIT_VERIFY','RETRY') " +
                        "AND next_retry_at <= CURRENT_TIMESTAMP(3) ORDER BY next_retry_at LIMIT ?",
                new CacheSyncEventRowMapper(), limit);
    }

    public Integer findVoucherStock(String voucherId) {
        List<Integer> values = jdbcTemplate.query(
                "SELECT stock FROM tb_seckill_voucher WHERE voucher_id=?",
                (rs, rowNum) -> rs.getInt(1), voucherId);
        return values.isEmpty() ? null : values.get(0);
    }

    private static Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }

    private static class CacheSyncEventRowMapper implements RowMapper<CacheSyncEvent> {
        @Override
        public CacheSyncEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            CacheSyncEvent event = new CacheSyncEvent();
            event.setEventId(rs.getString("event_id"));
            event.setSourceLogFile(rs.getString("source_log_file"));
            event.setSourceLogOffset(rs.getLong("source_log_offset"));
            event.setSchemaName(rs.getString("schema_name"));
            event.setTableName(rs.getString("table_name"));
            event.setRowKey(rs.getString("row_key"));
            event.setOperation(rs.getString("operation"));
            event.setCacheKey(rs.getString("cache_key"));
            event.setSyncMode(rs.getString("sync_mode"));
            event.setStatus(rs.getString("status"));
            event.setRetryCount(rs.getInt("retry_count"));
            Timestamp nextRetryAt = rs.getTimestamp("next_retry_at");
            event.setNextRetryAt(nextRetryAt == null ? null : nextRetryAt.toLocalDateTime());
            event.setLastError(rs.getString("last_error"));
            return event;
        }
    }
}
