package com.CityTop.websocket;

import com.alibaba.fastjson.JSON;
import com.CityTop.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Keeps only online WebSocket sessions. It is local to one application instance.
 */
@Component
@Slf4j
public class NotificationSessionRegistry {

    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int SEND_BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        CopyOnWriteArraySet<WebSocketSession> userSessions = sessions.computeIfAbsent(
                userId,
                ignored -> new CopyOnWriteArraySet<>()
        );
        // A thread pool can make multiple notification tasks reach the same connection concurrently.
        // This decorator serializes socket writes and bounds the buffered payload size.
        userSessions.removeIf(existing -> Objects.equals(existing.getId(), session.getId()));
        userSessions.add(new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MILLIS,
                SEND_BUFFER_SIZE_LIMIT_BYTES
        ));
    }

    public void unregister(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.removeIf(existing -> Objects.equals(existing.getId(), session.getId()));
        if (userSessions.isEmpty()) {
            sessions.remove(userId, userSessions);
        }
    }

    public int send(Long userId, NotificationMessage message) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return 0;
        }
        int delivered = 0;
        String payload = JSON.toJSONString(message);
        for (WebSocketSession session : userSessions) {
            if (!session.isOpen()) {
                unregister(userId, session);
                continue;
            }
            try {
                session.sendMessage(new TextMessage(payload));
                delivered++;
            } catch (IOException | RuntimeException exception) {
                log.warn("WebSocket notification delivery failed, userId={}", userId, exception);
                unregister(userId, session);
            }
        }
        return delivered;
    }

    int connectionCount(Long userId) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        return userSessions == null ? 0 : userSessions.size();
    }
}
