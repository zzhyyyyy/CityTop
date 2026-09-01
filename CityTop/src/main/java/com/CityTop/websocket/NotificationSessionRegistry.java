package com.CityTop.websocket;

import com.alibaba.fastjson.JSON;
import com.CityTop.dto.NotificationMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Keeps only online WebSocket sessions. It is local to one application instance.
 */
@Component
@Slf4j
public class NotificationSessionRegistry {

    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        sessions.computeIfAbsent(userId, ignored -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void unregister(Long userId, WebSocketSession session) {
        Set<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
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
            } catch (IOException exception) {
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
