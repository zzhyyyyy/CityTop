package com.CityTop.service.impl;

import com.CityTop.config.RocketMQConfig;
import com.CityTop.dto.Result;
import com.CityTop.entity.VoucherOrder;
import com.CityTop.mapper.VoucherOrderMapper;
import com.CityTop.mqmessage.VoucherOrderMessage;
import com.CityTop.service.ISeckillVoucherService;
import com.CityTop.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.CityTop.service.IVoucherService;
import com.CityTop.utils.RedisIdWorker;
import com.CityTop.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ISeckillVoucherService iSeckillVoucherService;
    @Autowired
    private IVoucherService voucherService;
    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }
    //订单队列
//    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024 * 1024);
//    private final static ExecutorService seckill_order_executor = Executors.newSingleThreadExecutor();
//    private IVoucherOrderService proxy;
//    @PostConstruct
//    private void init() {
//        seckill_order_executor.submit(new VoucherOrderTask());
//    }
//    private class VoucherOrderTask implements Runnable {
//        @Override
//        public void run() {
//            while(true) {
//                try {
//                    //取出队列中的订单
//                    VoucherOrder voucherOrder = orderTasks.take();
//                    handleVoucherOrder(voucherOrder);
//                } catch (Exception e) {
//                    log.error("处理订单异常", e);
//                }
//            }
//        }
//    }
//
//    private void handleVoucherOrder(VoucherOrder voucherOrder) {
//        Long userId = voucherOrder.getUserId();
//        RLock lock=redissonClient.getLock("lock:order:" + userId);
//        boolean isLock = lock.tryLock();
//        if(!isLock) {
//            log.error("获取锁失败，返回错误");
//            return;
//        }
//        try {
//            proxy = applicationContext.getBean(IVoucherOrderService.class);
//            proxy.createVoucherOrder(voucherOrder);
//        } finally {
//            lock.unlock();
//        }
//    }

    @Override
    public Result purchase(Long voucherId) {
        List<String> keys = new ArrayList<>();
        keys.add("seckill:stock:" + voucherId);
        keys.add("seckill:user:"  + voucherId);
        Long execute = stringRedisTemplate.execute(SECKILL_SCRIPT, keys, UserHolder.getUser().getId().toString());
        int result = execute.intValue();
        if(result==1) {
            return Result.fail("库存不足");
        }
        else if(result==2){
            return Result.fail("不能重复下单");
        }
        VoucherOrder voucherOrder = new VoucherOrder();
        long order = redisIdWorker.nextId("order");
//        voucherOrder.setId(order);
//        voucherOrder.setUserId(UserHolder.getUser().getId());
//        voucherOrder.setVoucherId(voucherId);
//        orderTasks.add(voucherOrder);
        VoucherOrderMessage message = VoucherOrderMessage.builder()
                .voucherId(voucherId)
                .userId(UserHolder.getUser().getId())
                .orderId(order)
                .createTime(LocalDateTime.now())
                .retryCount(0)
                .build();
        rocketMQTemplate.convertAndSend(
                RocketMQConfig.TOPIC + ":" + RocketMQConfig.TAG_ORDER,
                message
        );
        return Result.ok(order);
    }
//    @Override
//    public Result purchase(Long voucherId) {
//        DistributeLockImpl distributeLock = new DistributeLockImpl(stringRedisTemplate,"order:");
//        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
//        LocalDateTime now = LocalDateTime.now();
//        if (now.isBefore(voucher.getBeginTime()) || now.isAfter(voucher.getEndTime())) {
//            // 不在活动时间
//            return Result.fail("不在活动时间");
//        }
//        if (voucher.getStock() < 1) {
//            // 库存不足
//            return Result.fail("库存不足");
//        }
//        Long userId = UserHolder.getUser().getId();
//        RLock lock = redissonClient.getLock("lock:order:" + userId);
//
//        boolean isLock = false;
//        try {
//            // ✅ 带等待时间，不携带过期时间，看门狗自动续期
//            isLock = lock.tryLock(1, TimeUnit.SECONDS);
//
//            if (!isLock) {
//                return Result.fail("正在处理中，请勿重复下单");
//            }
//            log.debug("获取锁成功，用户: {}", userId);
//            IVoucherOrderService proxy = applicationContext.getBean(IVoucherOrderService.class);
//            return proxy.createVoucherOrder(voucherId);
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            log.error("获取锁被中断，用户: {}", userId, e);
//            return Result.fail("系统繁忙，请稍后重试");
//        } catch (Exception e) {
//            log.error("下单异常，用户: {}", userId, e);
//            return Result.fail("下单失败");
//        } finally {
//            // ✅ 只有获取成功且当前线程持有锁时才释放
//            if (isLock && lock.isHeldByCurrentThread()) {
//                lock.unlock();
//            }
//        }
//
////        synchronized (userId.toString().intern()) {
////            // 通过代理对象调用，事务生效
////            IVoucherOrderService proxy = applicationContext.getBean(IVoucherOrderService.class);
////            //通过Aop Context获取代理对象调用
////            //IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
////            return proxy.createVoucherOrder(voucherId);
////        }
//    }

    //解决一人多买 和 超卖 问题
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder (VoucherOrder voucherOrder) {
        Long userId = voucherOrder.getUserId();
        //数据库判断当前用户是否下过单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder.getVoucherId()).count();
        if (count > 0) {
            log.error("同一名用户不能重复下单");
        }
        // 乐观锁
        boolean updateSuccess = iSeckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherOrder.getVoucherId())
                .gt("stock", 0)
                .update();
        if (!updateSuccess) {
            // 扣减库存失败
            log.error("库存不足");
        }
        save(voucherOrder);
    }
}
