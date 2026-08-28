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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.dao.DuplicateKeyException;

import javax.annotation.Resource;
import java.util.Arrays;
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
    private static final String PROCESSED_KEY_PREFIX = "order:processed:";
    private static final String STATUS_KEY_PREFIX = "order:status:";
    private static final DefaultRedisScript<Long> SECKILL_ROLLBACK_SCRIPT;

    static {
        SECKILL_ROLLBACK_SCRIPT = new DefaultRedisScript<>();
        SECKILL_ROLLBACK_SCRIPT.setLocation(new ClassPathResource("seckill_rollback.lua"));
        SECKILL_ROLLBACK_SCRIPT.setResultType(Long.class);
    }

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
            String processedKey = PROCESSED_KEY_PREFIX + message.getOrderId();
            if (Boolean.TRUE.equals(redisTemplate.hasKey(processedKey)) || orderExists(message)) {
                log.warn("订单已处理，跳过: {}", message.getOrderId());
                markOrderSuccess(message);
                return;
            }

            VoucherOrder order = message.toVoucherOrder();
            voucherOrderService.createVoucherOrder(order);
            markOrderSuccess(message);

            log.info("订单处理成功: {}", message.getOrderId());
        } catch (DuplicateKeyException | IllegalStateException e) {
            if (orderExists(message)) {
                log.info("订单已由其他消息完成创建: {}", message.getOrderId());
                markOrderSuccess(message);
                return;
            }
            handleFailure(message, reconsumeTimes, e);
        } catch (Exception e) {
            handleFailure(message, reconsumeTimes, e);
        }
    }

    private boolean orderExists(VoucherOrderMessage message) {
        return voucherOrderService.getById(message.getOrderId()) != null;
    }

    private void markOrderSuccess(VoucherOrderMessage message) {
        redisTemplate.opsForValue().set(
                PROCESSED_KEY_PREFIX + message.getOrderId(),
                "1",
                30, TimeUnit.MINUTES
        );
        redisTemplate.opsForValue().set(
                STATUS_KEY_PREFIX + message.getOrderId(),
                JSON.toJSONString(Result.ok("下单成功")),
                30, TimeUnit.MINUTES
        );
    }

    private void handleFailure(VoucherOrderMessage message, int reconsumeTimes, Exception e) {
        log.error("订单处理失败: {}, reconsumeTimes={}", message.getOrderId(), reconsumeTimes, e);
        if (reconsumeTimes >= MAX_RETRY - 1) {
            if (orderExists(message)) {
                markOrderSuccess(message);
                return;
            }
            rollbackRedisState(message);
            return;
        }
        throw new RuntimeException("consume failed, retry later", e);
    }

    private void rollbackRedisState(VoucherOrderMessage message) {
        Long userId = message.getUserId();
        Long voucherId = message.getVoucherId();
        Long rollbackResult = redisTemplate.execute(
                SECKILL_ROLLBACK_SCRIPT,
                Arrays.asList("seckill:stock:" + voucherId, "seckill:user:" + voucherId),
                String.valueOf(userId)
        );

        redisTemplate.opsForValue().set(
                STATUS_KEY_PREFIX + message.getOrderId(),
                JSON.toJSONString(Result.fail("下单失败，请重试")),
                30, TimeUnit.MINUTES
        );
        log.error("订单失败已回滚Redis状态: orderId={}, rollbackResult={}", message.getOrderId(), rollbackResult);
    }
}
