CREATE TABLE IF NOT EXISTS cache_sync_event (
  event_id VARCHAR(160) NOT NULL COMMENT 'binlog事件唯一标识',
  source_log_file VARCHAR(128) NOT NULL COMMENT 'binlog文件名',
  source_log_offset BIGINT NOT NULL COMMENT 'binlog偏移量',
  schema_name VARCHAR(64) NOT NULL,
  table_name VARCHAR(64) NOT NULL,
  row_key VARCHAR(255) NOT NULL COMMENT '业务主键',
  operation VARCHAR(16) NOT NULL COMMENT 'INSERT/UPDATE/DELETE',
  cache_key VARCHAR(255) DEFAULT NULL,
  sync_mode VARCHAR(32) NOT NULL COMMENT 'CACHE_INVALIDATE/RECONCILE_SIGNAL',
  status VARCHAR(32) NOT NULL COMMENT 'PENDING/WAIT_VERIFY/SUCCESS/RETRY/MANUAL_REQUIRED',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME(3) DEFAULT NULL,
  last_error VARCHAR(1000) DEFAULT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (event_id),
  KEY idx_cache_sync_due (status, next_retry_at),
  KEY idx_cache_sync_source (table_name, row_key, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Canal缓存同步审计与补偿事件';
