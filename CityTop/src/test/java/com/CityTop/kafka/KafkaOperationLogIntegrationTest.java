package com.CityTop.kafka;

import com.CityTop.config.KafkaOperationLogConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KafkaOperationLogIntegrationTest {

    @Test
    void shouldSendAndConsumeWithLocalKafka() throws Exception {
        Assumptions.assumeTrue(Boolean.getBoolean("citytop.kafka.integration-test"),
                "仅在显式开启本地 Kafka 集成测试时执行");

        OperationLogProperties properties = new OperationLogProperties();
        KafkaOperationLogConfig config = new KafkaOperationLogConfig();
        ProducerFactory<String, String> producerFactory =
                config.kafkaOperationLogProducerFactory(properties);
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        String eventId = "integration-" + UUID.randomUUID();
        String payload = "{\"eventId\":\"" + eventId + "\"}";

        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers());
        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "citytop-integration-" + UUID.randomUUID());
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerConfig)) {
            consumer.subscribe(Collections.singletonList(properties.getTopic()));
            template.send(properties.getTopic(), eventId, payload).get();

            boolean found = false;
            long deadline = System.currentTimeMillis() + 10000;
            while (!found && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    if (payload.equals(record.value())) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found, "应当能从本地 Kafka 读取刚刚发送的日志消息");
        } finally {
            template.destroy();
            ((DefaultKafkaProducerFactory<String, String>) producerFactory).destroy();
        }
    }
}
