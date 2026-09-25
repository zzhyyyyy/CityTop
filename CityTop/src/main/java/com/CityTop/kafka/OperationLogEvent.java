package com.CityTop.kafka;

public class OperationLogEvent {
    private final String eventId;
    private final String traceId;
    private final String service;
    private final Long userId;
    private final String method;
    private final String path;
    private final int status;
    private final long durationMs;
    private final long occurredAt;
    private final boolean successful;
    private final String exceptionType;

    public OperationLogEvent(String eventId, String traceId, String service, Long userId,
                             String method, String path, int status, long durationMs,
                             long occurredAt, boolean successful, String exceptionType) {
        this.eventId = eventId;
        this.traceId = traceId;
        this.service = service;
        this.userId = userId;
        this.method = method;
        this.path = path;
        this.status = status;
        this.durationMs = durationMs;
        this.occurredAt = occurredAt;
        this.successful = successful;
        this.exceptionType = exceptionType;
    }

    public String getEventId() { return eventId; }
    public String getTraceId() { return traceId; }
    public String getService() { return service; }
    public Long getUserId() { return userId; }
    public String getMethod() { return method; }
    public String getPath() { return path; }
    public int getStatus() { return status; }
    public long getDurationMs() { return durationMs; }
    public long getOccurredAt() { return occurredAt; }
    public boolean isSuccessful() { return successful; }
    public String getExceptionType() { return exceptionType; }
}
