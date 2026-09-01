package com.CityTop.websocket;

import com.CityTop.dto.NotificationMessage;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationSessionRegistryTest {

    @Test
    void shouldDeliverToEveryActiveSessionAndRemoveClosedSessions() throws Exception {
        NotificationSessionRegistry registry = new NotificationSessionRegistry();
        WebSocketSession active = mock(WebSocketSession.class);
        WebSocketSession closed = mock(WebSocketSession.class);
        when(active.isOpen()).thenReturn(true);
        when(closed.isOpen()).thenReturn(false);
        registry.register(1L, active);
        registry.register(1L, closed);

        int delivered = registry.send(1L, new NotificationMessage("TEST", "hello", 1L));

        assertEquals(1, delivered);
        assertEquals(1, registry.connectionCount(1L));
        verify(active).sendMessage(any());
    }
}
