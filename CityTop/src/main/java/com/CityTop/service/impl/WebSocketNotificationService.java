package com.CityTop.service.impl;

import com.CityTop.dto.NotificationMessage;
import com.CityTop.service.NotificationService;
import com.CityTop.websocket.NotificationSessionRegistry;
import org.springframework.stereotype.Service;

@Service
public class WebSocketNotificationService implements NotificationService {

    private final NotificationSessionRegistry sessionRegistry;

    public WebSocketNotificationService(NotificationSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void notifyUser(Long userId, String type, String content) {
        sessionRegistry.send(userId, new NotificationMessage(type, content, System.currentTimeMillis()));
    }
}
