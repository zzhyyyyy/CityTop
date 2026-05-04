package com.CityTop.MQListener;

import com.alibaba.fastjson.JSON;
import com.CityTop.config.RocketMQConfig;
import com.CityTop.dto.Result;
import com.CityTop.entity.VoucherOrder;
import com.CityTop.mqmessage.VoucherOrderMessage;
import com.CityTop.service.IVoucherOrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RocketMQMessageListener(
        topic = RocketMQConfig.TOPIC,
        selectorExpression = RocketMQConfig.TAG_ORDER,
        consumerGroup = RocketMQConfig.CONSUMER_GROUP,
        messageModel = MessageModel.CLUSTERING,
        consumeMode = ConsumeMode.CONCURRENTLY
        // maxReconsumeTimes = 3 最大重试次数
        // 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
        // delayLevelWhenNextConsume = 3 每次重试延迟时间
)
public class VoucherOrderConsumer implements RocketMQListener<MessageExt> {

    private static final int MAX_RETRY = 3;

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public void onMessage(MessageExt messageExt) {
        VoucherOrderMessage message = JSON.parseObject(messageExt.getBody(), VoucherOrderMessage.class);
        int reconsumeTimes = messageExt.getReconsumeTimes();
        try {
            log.info("收到订单消息: {}, reconsumeTimes={}", message.getOrderId(), reconsumeTimes);
            // int i = 1/0; 测试rocketmq的重试机制
            String processedKey = "order:processed:" + message.getOrderId();
            Boolean processed = redisTemplate.opsForValue()
                    .setIfAbsent(processedKey, "1", 5, TimeUnit.MINUTES);

            if (Boolean.FALSE.equals(processed)) {
                log.warn("订单已处理，跳过: {}", message.getOrderId());
                return;
            }

            VoucherOrder order = message.toVoucherOrder();
            voucherOrderService.createVoucherOrder(order);

            redisTemplate.opsForValue().set(
                    "order:status:" + message.getOrderId(),
                    JSON.toJSONString(Result.ok("下单成功")),
                    30, TimeUnit.MINUTES
            );

            log.info("订单处理成功: {}", message.getOrderId());
        } catch (Exception e) {
            log.error("订单处理失败: {}, reconsumeTimes={}", message.getOrderId(), reconsumeTimes, e);
            if (reconsumeTimes >= MAX_RETRY - 1) {
                rollbackRedisState(message);
                return;
            }
            throw new RuntimeException("consume failed, retry later");
        }
    }

    private void rollbackRedisState(VoucherOrderMessage message) {
        Long userId = message.getUserId();
        Long voucherId = message.getVoucherId();
        String stockKey = "seckill:stock:" + voucherId;
        String userSetKey = "seckill:user:" + voucherId;

        String stockValue = redisTemplate.opsForValue().get(stockKey);
        long stock = stockValue == null ? 0L : Long.parseLong(stockValue);
        redisTemplate.opsForValue().set(stockKey, String.valueOf(stock + 1));
        redisTemplate.opsForSet().remove(userSetKey, String.valueOf(userId));

        redisTemplate.opsForValue().set(
                "order:status:" + message.getOrderId(),
                JSON.toJSONString(Result.fail("下单失败，请重试")),
                30, TimeUnit.MINUTES
        );
        log.error("订单失败已回滚Redis状态: orderId={}", message.getOrderId());
    }
}
