package com.CityTop.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Connection lifecycle for personal notification channels.
 */
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final NotificationSessionRegistry sessionRegistry;

    public NotificationWebSocketHandler(NotificationSessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(userId(session), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Notification connections are server-push only.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(userId(session), session);
    }

    private Long userId(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }
}
