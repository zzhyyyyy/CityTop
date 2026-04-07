package com.CityTop.MQListener;

import com.alibaba.fastjson.JSON;
import com.CityTop.config.RabbitMQConfig;
import com.CityTop.dto.Result;
import com.CityTop.entity.VoucherOrder;
import com.CityTop.rabbitmqMessageConvert.VoucherOrderMessage;
import com.CityTop.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class VoucherOrderConsumer {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(VoucherOrderConsumer.class);
    @Resource
    private IVoucherOrderService voucherOrderService;
    //注意 这里用@Resource就错了 因为@Resource注解是先根据属性名进行匹配 会先匹配到RedisTemplate
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 消费秒杀订单消息
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_NAME,
            containerFactory = "rabbitListenerContainerFactory")
    public void handleOrder(VoucherOrderMessage message,
                            Message mqMessage,
                            Channel channel) throws IOException {

        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();

        try {
            log.info("收到订单消息: {}", message.getOrderId());

            // 1. 唯一消息ID处理幂等性检查（防止重复消费）
            String processedKey = "order:processed:" + message.getOrderId();
            Boolean processed = redisTemplate.opsForValue()
                    .setIfAbsent(processedKey, "1", 5, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(processed)) {
                log.warn("订单已处理，跳过: {}", message.getOrderId());
                channel.basicAck(deliveryTag, false);  // 确认消息
                return;
            }

            // 2. 数据库操作（事务）
            VoucherOrder order = message.toVoucherOrder();
            voucherOrderService.createVoucherOrder(order);

            // 3. 更新Redis订单状态（供前端轮询）
            redisTemplate.opsForValue().set(
                    "order:status:" + message.getOrderId(),
                    JSON.toJSONString(Result.ok("下单成功")),
                    30, TimeUnit.MINUTES
            );

            // 4. 手动确认消息
            channel.basicAck(deliveryTag, false);
            log.info("订单处理成功: {}", message.getOrderId());

        } catch (Exception e) {
            log.error("订单处理失败: {}", message.getOrderId(), e);
            handleError(message, mqMessage, channel, deliveryTag, e);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.DLX_QUEUE_NAME,
            containerFactory = "rabbitListenerContainerFactory")
    public void handleDlxOrder(VoucherOrderMessage message,
                               Message mqMessage,
                               Channel channel) throws IOException {
        log.info("收到死信订单消息: {}", message.getOrderId());
        long deliveryTag = mqMessage.getMessageProperties().getDeliveryTag();
        long userId = message.getUserId();
        long voucherId = message.getVoucherId();
        log.info("消息处理失败，还原redis状态");
        String s = redisTemplate.opsForValue().get("seckill:stock:" + voucherId);
        long stock = 0;
        if (s != null) {
            stock = Long.parseLong(s);
        }
        redisTemplate.opsForValue().set("seckill:stock:"+voucherId, String.valueOf(stock+1));
        redisTemplate.opsForSet().remove("seckill:user:"+voucherId, String.valueOf(userId));
        channel.basicAck(deliveryTag, false);
    }

    /**
     * 错误处理：重试或进入死信队列
     */
    private void handleError(VoucherOrderMessage message, Message mqMessage,
                             Channel channel, long deliveryTag, Exception e)
            throws IOException {

        int retryCount = message.getRetryCount();

        if (retryCount < 3) {
            // 重试：重新入队（延迟重试更好）
            message.setRetryCount(retryCount + 1);
            log.warn("订单处理失败，第{}次重试: {}", retryCount + 1, message.getOrderId());

            // 拒绝消息，重新入队
            // long deliveryTag,   // 1. 消息的唯一标识
            // boolean multiple,   // 2. 是否批量处理
            // boolean requeue     // 3. 是否重新入队
            channel.basicNack(deliveryTag, false, true);
        } else {
            // 超过重试次数，进入死信队列
            log.error("订单处理失败超过3次，进入死信队列: {}", message.getOrderId());

            channel.basicNack(deliveryTag, false, false);  // 不重新入队，进入死信
        }
    }
}