package com.CityTop.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 个人通知通道的连接生命周期处理器。
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
        // 通知连接仅支持服务端主动推送。
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(userId(session), session);
    }

    private Long userId(WebSocketSession session) {
        return (Long) session.getAttributes().get("userId");
    }
}
