package com.CityTop.service.impl;

import com.CityTop.dto.NotificationMessage;
import com.CityTop.service.NotificationService;
import com.CityTop.websocket.NotificationSessionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class WebSocketNotificationService implements NotificationService {

    private final NotificationSessionRegistry sessionRegistry;
    private final TaskExecutor notificationExecutor;

    public WebSocketNotificationService(NotificationSessionRegistry sessionRegistry,
                                        @Qualifier("webSocketNotificationExecutor") TaskExecutor notificationExecutor) {
        this.sessionRegistry = sessionRegistry;
        this.notificationExecutor = notificationExecutor;
    }

    @Override
    public void notifyUser(Long userId, String type, String content) {
        NotificationMessage message = new NotificationMessage(type, content, System.currentTimeMillis());
        try {
            notificationExecutor.execute(() -> {
                try {
                    sessionRegistry.send(userId, message);
                } catch (RuntimeException exception) {
                    log.warn("WebSocket notification task failed, userId={}, type={}", userId, type, exception);
                }
            });
        } catch (TaskRejectedException exception) {
            // Do not turn a successful domain operation into a failed request for an online-only push.
            log.warn("WebSocket notification queue is full; skipping online delivery, userId={}, type={}", userId, type);
        }
    }
}
