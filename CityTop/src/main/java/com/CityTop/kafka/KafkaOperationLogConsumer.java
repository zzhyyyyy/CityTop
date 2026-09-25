package com.CityTop.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "citytop.kafka.operation-log", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class KafkaOperationLogConsumer {
    private static final Logger operationLog = LoggerFactory.getLogger("KAFKA_OPERATION_LOG");

    /**
     * 消费方法正常返回后，容器按 RECORD 模式提交这条消息的 offset。
     */
    @KafkaListener(
            topics = "${citytop.kafka.operation-log.topic:citytop-operation-log}",
            groupId = "${citytop.kafka.operation-log.group-id:citytop-operation-log-writer}")
    public void consume(ConsumerRecord<String, String> record) {
        operationLog.info(record.value());
    }
}
