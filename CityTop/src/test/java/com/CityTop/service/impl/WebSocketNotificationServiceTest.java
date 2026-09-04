package com.CityTop.service.impl;

import com.CityTop.websocket.NotificationSessionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class WebSocketNotificationServiceTest {

    @Test
    void shouldDelegateOnlineNotificationToDedicatedExecutor() {
        NotificationSessionRegistry registry = mock(NotificationSessionRegistry.class);
        AtomicBoolean executed = new AtomicBoolean(false);
        TaskExecutor executor = task -> {
            executed.set(true);
            task.run();
        };
        WebSocketNotificationService service = new WebSocketNotificationService(registry, executor);

        service.notifyUser(1L, "BLOG_LIKED", "hello");

        assertTrue(executed.get());
        verify(registry).send(org.mockito.Mockito.eq(1L), org.mockito.Mockito.any());
    }

    @Test
    void shouldNotFailCallerWhenOnlineNotificationIsRejected() {
        NotificationSessionRegistry registry = mock(NotificationSessionRegistry.class);
        TaskExecutor rejectingExecutor = task -> {
            throw new TaskRejectedException("full");
        };
        WebSocketNotificationService service = new WebSocketNotificationService(registry, rejectingExecutor);

        assertDoesNotThrow(() -> service.notifyUser(1L, "BLOG_LIKED", "hello"));
    }
}
