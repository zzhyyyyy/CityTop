package com.CityTop.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "citytop.kafka.operation-log", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class KafkaOperationLogPublisher {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final TaskExecutor executor;
    private final OperationLogProperties properties;

    public KafkaOperationLogPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                      ObjectMapper objectMapper,
                                      @Qualifier("kafkaOperationLogExecutor") TaskExecutor executor,
                                      OperationLogProperties properties) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.executor = executor;
        this.properties = properties;
    }

    /**
     * HTTP 线程只负责把任务放入有界队列，不等待 Kafka 元数据或 Broker ACK。
     * 队列满或 Kafka 不可用时降级记录告警，不反向影响核心业务。
     */
    public void publish(OperationLogEvent event) {
        try {
            executor.execute(() -> send(event));
        } catch (TaskRejectedException ex) {
            log.warn("Kafka 操作日志线程池已满，丢弃非核心日志，eventId={}", event.getEventId());
        }
    }

    private void send(OperationLogEvent event) {
        final String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.warn("Kafka 操作日志序列化失败，eventId={}", event.getEventId(), ex);
            return;
        }

        String key = event.getUserId() == null ? event.getEventId() : String.valueOf(event.getUserId());
        try {
            ListenableFuture<SendResult<String, String>> future =
                    kafkaTemplate.send(properties.getTopic(), key, payload);
            future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
                @Override
                public void onFailure(Throwable ex) {
                    log.warn("Kafka 操作日志发送失败，eventId={}", event.getEventId(), ex);
                }

                @Override
                public void onSuccess(SendResult<String, String> result) {
                    log.debug("Kafka 操作日志发送成功，eventId={}", event.getEventId());
                }
            });
        } catch (RuntimeException ex) {
            log.warn("Kafka 操作日志提交失败，eventId={}", event.getEventId(), ex);
        }
    }
}
