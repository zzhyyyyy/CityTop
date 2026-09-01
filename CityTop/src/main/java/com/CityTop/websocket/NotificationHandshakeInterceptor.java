package com.CityTop.websocket;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

import static com.CityTop.utils.RedisConstants.LOGIN_USER_KEY;

/**
 * Validates the existing login token before upgrading an HTTP request to WebSocket.
 */
@Component
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {

    private final StringRedisTemplate redisTemplate;

    public NotificationHandshakeInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = request.getHeaders().getFirst("token");
        if (!StringUtils.hasText(token)) {
            token = UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams().getFirst("token");
        }
        if (!StringUtils.hasText(token)) {
            return false;
        }
        Map<Object, Object> user = redisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        Object userId = user.get("id");
        if (user.isEmpty() || userId == null) {
            return false;
        }
        attributes.put("userId", Long.valueOf(userId.toString()));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // Session registration is handled by the WebSocket handler.
    }
}
