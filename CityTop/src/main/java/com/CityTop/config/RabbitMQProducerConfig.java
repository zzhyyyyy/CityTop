package com.CityTop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class RabbitMQProducerConfig {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @PostConstruct
    public void init() {
        // 确认回调：消息到达交换机
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if(ack)  log.info("消息发送成功: {}", correlationData.getId());
            if (!ack) {
                log.error("消息发送到交换机失败: {}, 原因: {}", 
                    correlationData.getId(), cause);
                // TODO: 补偿机制，记录到数据库定时重试
            }
        });
        
        // 退回回调：消息到达交换机但未到达队列
        // ... existing code ...
        // 退回回调：消息到达交换机但未到达队列
        rabbitTemplate.setReturnCallback((message, replyCode, replyText, exchange, routingKey) -> {
            log.error("消息路由到队列失败: {}, 交换机: {}, 路由键: {}, 原因: {}",
                message,
                exchange,
                routingKey,
                replyText);
            // TODO: 补偿处理
        });

    }
}