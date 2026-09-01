package com.CityTop.config;

import com.CityTop.websocket.NotificationHandshakeInterceptor;
import com.CityTop.websocket.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationHandshakeInterceptor notificationHandshakeInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(NotificationWebSocketHandler notificationWebSocketHandler,
                           NotificationHandshakeInterceptor notificationHandshakeInterceptor,
                           @Value("${citytop.websocket.allowed-origins:http://localhost:8080,http://localhost:5173}") String allowedOrigins) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.notificationHandshakeInterceptor = notificationHandshakeInterceptor;
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(notificationHandshakeInterceptor)
                .setAllowedOrigins(allowedOrigins);
    }
}
