package com.CityTop.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // 交换机名称
    public static final String EXCHANGE_NAME = "seckill.exchange";
    // 队列名称
    public static final String QUEUE_NAME = "seckill.queue";
    // 路由键
    public static final String ROUTING_KEY = "seckill.order";
    //死信队列名称
    public static final String DLX_QUEUE_NAME = "seckill.dlx.queue"; //死信队列名称
    /**
     * 交换机：Direct模式，精确匹配路由键
     */
    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }
    
    /**
     * 队列：持久化，非排他，非自动删除
     */
    @Bean
    public Queue seckillQueue() {
        return QueueBuilder.durable(QUEUE_NAME)
                // 死信交换机配置（处理失败消息）
                .withArgument("x-dead-letter-exchange", "seckill.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "seckill.dlx.key")
                // 队列消息过期时间（可选）
                // .withArgument("x-message-ttl", 60000)
                .build();
    }
    
    /**
     * 绑定关系
     */
    @Bean
    public Binding seckillBinding() {
        return BindingBuilder.bind(seckillQueue())
                .to(seckillExchange())
                .with(ROUTING_KEY);
    }
    
    // ========== 死信队列配置（处理异常订单） ==========
    
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("seckill.dlx.exchange", true, false);
    }
    
    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable("seckill.dlx.queue").build();
    }
    
    @Bean
    public Binding dlxBinding() {
        return BindingBuilder.bind(dlxQueue())
                .to(dlxExchange())
                .with("seckill.dlx.key");
    }
}