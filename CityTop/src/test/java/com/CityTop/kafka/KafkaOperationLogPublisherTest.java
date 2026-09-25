package com.CityTop.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.SettableListenableFuture;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaOperationLogPublisherTest {

    @SuppressWarnings("unchecked")
    @Test
    void shouldSerializeAndSendByDedicatedExecutor() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        SettableListenableFuture<SendResult<String, String>> future =
                new SettableListenableFuture<>();
        future.set(mock(SendResult.class));
        when(template.send(anyString(), anyString(), anyString())).thenReturn(future);
        TaskExecutor directExecutor = Runnable::run;
        OperationLogProperties properties = new OperationLogProperties();
        KafkaOperationLogPublisher publisher = new KafkaOperationLogPublisher(
                template, new ObjectMapper(), directExecutor, properties);

        publisher.publish(event());

        verify(template).send(
                org.mockito.ArgumentMatchers.eq("citytop-operation-log"),
                org.mockito.ArgumentMatchers.eq("7"),
                org.mockito.ArgumentMatchers.contains("\"path\":\"/shop/1\""));
    }

    @Test
    void shouldNotFailCallerWhenQueueIsFull() {
        KafkaTemplate<String, String> template = mock(KafkaTemplate.class);
        TaskExecutor rejectingExecutor = task -> {
            throw new TaskRejectedException("queue full");
        };
        KafkaOperationLogPublisher publisher = new KafkaOperationLogPublisher(
                template, new ObjectMapper(), rejectingExecutor, new OperationLogProperties());

        assertDoesNotThrow(() -> publisher.publish(event()));
        verify(template, never()).send(anyString(), anyString(), anyString());
    }

    private static OperationLogEvent event() {
        return new OperationLogEvent(
                "event-1", "trace-1", "CityTop", 7L, "GET", "/shop/1",
                200, 15L, System.currentTimeMillis(), true, null);
    }
}
