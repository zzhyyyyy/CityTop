package com.CityTop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 将尽力而为的在线 WebSocket 推送与 HTTP 请求处理线程隔离。
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

        // 在线实时通知属于尽力而为的能力；线程池拒绝任务时由调用方记录并降级。
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(shutdownAwaitSeconds);
        return executor;
    }
}
