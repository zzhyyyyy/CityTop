package com.citytop.canalsync.canal;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.Message;
import com.citytop.canalsync.config.CanalProperties;
import com.citytop.canalsync.service.CanalEventProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CanalClientLifecycle implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(CanalClientLifecycle.class);

    private final CanalProperties properties;
    private final CanalEventProcessor processor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "citytop-canal-consumer");
        thread.setDaemon(false);
        return thread;
    });

    private volatile CanalConnector connector;

    public CanalClientLifecycle(CanalProperties properties, CanalEventProcessor processor) {
        this.properties = properties;
        this.processor = processor;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            executor.submit(this::consumeLoop);
        }
    }

    private void consumeLoop() {
        while (running.get()) {
            try {
                ensureConnected();
                Message message = connector.getWithoutAck(
                        properties.getBatchSize(),
                        properties.getPollingTimeoutMillis(),
                        TimeUnit.MILLISECONDS);
                long batchId = message.getId();
                if (batchId == -1 || message.getEntries().isEmpty()) {
                    continue;
                }

                try {
                    processor.process(message.getEntries());
                    connector.ack(batchId);
                } catch (Exception ex) {
                    connector.rollback(batchId);
                    log.error("处理 Canal 批次失败，已回滚游标等待重放，batchId={}", batchId, ex);
                    closeConnector();
                    sleepBeforeReconnect();
                }
            } catch (Exception ex) {
                log.warn("连接或拉取 Canal 失败，将自动重连", ex);
                closeConnector();
                sleepBeforeReconnect();
            }
        }
        closeConnector();
    }

    private void ensureConnected() {
        if (connector != null) {
            return;
        }
        CanalConnector newConnector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(properties.getHost(), properties.getPort()),
                properties.getDestination(),
                properties.getUsername(),
                properties.getPassword());
        newConnector.connect();
        newConnector.subscribe(properties.getSubscription());
        newConnector.rollback();
        connector = newConnector;
        log.info("Canal 已连接，server={}:{}, destination={}, subscription={}",
                properties.getHost(), properties.getPort(),
                properties.getDestination(), properties.getSubscription());
    }

    private void closeConnector() {
        CanalConnector current = connector;
        connector = null;
        if (current != null) {
            try {
                current.disconnect();
            } catch (RuntimeException ex) {
                log.debug("关闭 Canal 连接时出现异常", ex);
            }
        }
    }

    private void sleepBeforeReconnect() {
        try {
            Thread.sleep(properties.getReconnectDelayMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            closeConnector();
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
