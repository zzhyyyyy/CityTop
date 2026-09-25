package com.CityTop.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "citytop.kafka.operation-log")
public class OperationLogProperties {
    private boolean enabled = true;
    private String bootstrapServers = "127.0.0.1:9092";
    private String topic = "citytop-operation-log";
    private String groupId = "citytop-operation-log-writer";
    private int partitions = 3;
    private short replicas = 1;
    private final Executor executor = new Executor();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getBootstrapServers() { return bootstrapServers; }
    public void setBootstrapServers(String bootstrapServers) { this.bootstrapServers = bootstrapServers; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public int getPartitions() { return partitions; }
    public void setPartitions(int partitions) { this.partitions = partitions; }
    public short getReplicas() { return replicas; }
    public void setReplicas(short replicas) { this.replicas = replicas; }
    public Executor getExecutor() { return executor; }

    public static class Executor {
        private int corePoolSize = 1;
        private int maxPoolSize = 2;
        private int queueCapacity = 500;
        private int keepAliveSeconds = 60;
        private int shutdownAwaitSeconds = 10;

        public int getCorePoolSize() { return corePoolSize; }
        public void setCorePoolSize(int corePoolSize) { this.corePoolSize = corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
        public int getQueueCapacity() { return queueCapacity; }
        public void setQueueCapacity(int queueCapacity) { this.queueCapacity = queueCapacity; }
        public int getKeepAliveSeconds() { return keepAliveSeconds; }
        public void setKeepAliveSeconds(int keepAliveSeconds) { this.keepAliveSeconds = keepAliveSeconds; }
        public int getShutdownAwaitSeconds() { return shutdownAwaitSeconds; }
        public void setShutdownAwaitSeconds(int shutdownAwaitSeconds) { this.shutdownAwaitSeconds = shutdownAwaitSeconds; }
    }
}
