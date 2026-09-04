package com.CityTop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Isolates best-effort online WebSocket delivery from request-processing threads.
 */
@Configuration
public class WebSocketNotificationExecutorConfig {

    @Bean("webSocketNotificationExecutor")
    public ThreadPoolTaskExecutor webSocketNotificationExecutor(
            @Value("${citytop.websocket.notification-executor.core-pool-size:2}") int corePoolSize,
            @Value("${citytop.websocket.notification-executor.max-pool-size:8}") int maxPoolSize,
            @Value("${citytop.websocket.notification-executor.queue-capacity:500}") int queueCapacity,
            @Value("${citytop.websocket.notification-executor.keep-alive-seconds:60}") int keepAliveSeconds,
            @Value("${citytop.websocket.notification-executor.shutdown-await-seconds:10}") int shutdownAwaitSeconds) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setThreadNamePrefix("ws-notify-");

        // Real-time online notifications are best-effort. Rejection is handled by the caller.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(shutdownAwaitSeconds);
        return executor;
    }
}
