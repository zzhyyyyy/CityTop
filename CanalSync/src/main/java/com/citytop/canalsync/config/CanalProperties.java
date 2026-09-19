package com.citytop.canalsync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "citytop.canal")
public class CanalProperties {
    private String host = "127.0.0.1";
    private int port = 11111;
    private String destination = "citytop";
    private String username = "";
    private String password = "";
    private String subscription = "sql_store\\.(tb_shop|tb_seckill_voucher|tb_voucher_order)";
    private int batchSize = 200;
    private long pollingTimeoutMillis = 1000;
    private long reconnectDelayMillis = 3000;
    private long verifyDelayMillis = 1500;
    private final Retry retry = new Retry();

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getSubscription() { return subscription; }
    public void setSubscription(String subscription) { this.subscription = subscription; }
    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    public long getPollingTimeoutMillis() { return pollingTimeoutMillis; }
    public void setPollingTimeoutMillis(long pollingTimeoutMillis) { this.pollingTimeoutMillis = pollingTimeoutMillis; }
    public long getReconnectDelayMillis() { return reconnectDelayMillis; }
    public void setReconnectDelayMillis(long reconnectDelayMillis) { this.reconnectDelayMillis = reconnectDelayMillis; }
    public long getVerifyDelayMillis() { return verifyDelayMillis; }
    public void setVerifyDelayMillis(long verifyDelayMillis) { this.verifyDelayMillis = verifyDelayMillis; }
    public Retry getRetry() { return retry; }

    public static class Retry {
        private long fixedDelayMillis = 2000;
        private int batchSize = 100;
        private int maxAttempts = 8;
        private int baseDelaySeconds = 2;

        public long getFixedDelayMillis() { return fixedDelayMillis; }
        public void setFixedDelayMillis(long fixedDelayMillis) { this.fixedDelayMillis = fixedDelayMillis; }
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        public int getMaxAttempts() { return maxAttempts; }
        public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
        public int getBaseDelaySeconds() { return baseDelaySeconds; }
        public void setBaseDelaySeconds(int baseDelaySeconds) { this.baseDelaySeconds = baseDelaySeconds; }
    }
}
