package com.citytop.canalsync.domain;

import java.time.LocalDateTime;

public class CacheSyncEvent {
    private String eventId;
    private String sourceLogFile;
    private long sourceLogOffset;
    private String schemaName;
    private String tableName;
    private String rowKey;
    private String operation;
    private String cacheKey;
    private String syncMode;
    private String status;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getSourceLogFile() { return sourceLogFile; }
    public void setSourceLogFile(String sourceLogFile) { this.sourceLogFile = sourceLogFile; }
    public long getSourceLogOffset() { return sourceLogOffset; }
    public void setSourceLogOffset(long sourceLogOffset) { this.sourceLogOffset = sourceLogOffset; }
    public String getSchemaName() { return schemaName; }
    public void setSchemaName(String schemaName) { this.schemaName = schemaName; }
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getRowKey() { return rowKey; }
    public void setRowKey(String rowKey) { this.rowKey = rowKey; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getCacheKey() { return cacheKey; }
    public void setCacheKey(String cacheKey) { this.cacheKey = cacheKey; }
    public String getSyncMode() { return syncMode; }
    public void setSyncMode(String syncMode) { this.syncMode = syncMode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}
