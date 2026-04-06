package com.CityTop.service.impl;

import com.alibaba.fastjson.JSON;
import com.CityTop.entity.RedisData;
import com.CityTop.entity.Shop;
import com.CityTop.mapper.ShopMapper;
import com.CityTop.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.CityTop.utils.DistributeLock;
import com.CityTop.utils.DistributeLockImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Override
    public Shop queryById(Long id) {
        // 基于缓存空对象解决缓存穿透问题 因为采用逻辑过期解决缓存击穿问题 多加了一个ExpiredTime 所以用queryCacheByThrougt和queryCacheByMutex无法正确解析Json对象了
        //return queryCacheByThrougt(id);
        // 基于互斥锁解决缓存击穿问题
        //return queryCacheByMutex(id);
        //基于逻辑过期解决缓存击穿问题
        return queryCacheByMutex(id);
    }

    public Shop queryCacheByThrougt(Long id){
        log.info("查询店铺信息，id：{}", id);
        String key = "shop:" + id;
        String shopJson = redisTemplate.opsForValue().get(key);
        if(shopJson!=null&&shopJson.length()>0)
        {
            log.info("店铺信息已存在，直接返回");
            return JSON.parseObject(shopJson, Shop.class);
        }
        else if(shopJson!=null)
        {
            log.info("店铺信息已删除");
            return null;
        }
        Shop shop = this.getById(id);
        if(shop==null)
        {
            log.info("店铺不存在");
            redisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
            return null;
        }
        redisTemplate.opsForValue().set(key, JSON.toJSONString(shop));
        redisTemplate.expire(key, 30L, TimeUnit.MINUTES);
        log.info("店铺信息不存在，写入缓存");
        return shop;
    }
    public Shop queryCacheByMutex(Long id) {
        log.info("查询店铺信息，id：{}", id);
        String key = "shop:" + id;
        String shopJson = redisTemplate.opsForValue().get(key);
        Shop shop = null;
        if (shopJson != null && shopJson.length() > 0) {
            log.info("店铺信息已存在，直接返回");
            System.out.println(shopJson);
            return JSON.parseObject(shopJson, Shop.class);
        }
        DistributeLock distributeLock = new DistributeLockImpl(redisTemplate, "lock:shop:"+ id);
        try {
            boolean isLock = distributeLock.tryLock(30L);
            if (!isLock) {
                log.info("获取锁失败，等待重试");
                Thread.sleep(50);
                return queryCacheByMutex(id);
            }
            //二次确认
            shopJson = redisTemplate.opsForValue().get(key);
            if (shopJson != null && shopJson.length() > 0) {
                log.info("店铺信息已存在，直接返回");
                return JSON.parseObject(shopJson, Shop.class);
            }
            shop = this.getById(id);
            if (shop == null) {
                log.info("店铺不存在");
                redisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
            }
            log.info("店铺信息不存在，写入缓存");
            redisTemplate.opsForValue().set(key, JSON.toJSONString(shop));
            redisTemplate.expire(key, 30L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            distributeLock.unlock();
        }
        return shop;
    }
//    public boolean tryLock(String key) {
//        Boolean flag = redisTemplate.opsForValue().setIfAbsent(key, "1");
//        return flag != null && flag;
//    }
//    public void unlock(String key) {
//        redisTemplate.delete(key);
//    }
    public Shop queryCacheByLogicalExpire(Long id) {
        log.info("查询店铺信息，id：{}", id);
        String key = "shop:" + id;
        String lock = "lock:shop:" + id;
        String shopJson = redisTemplate.opsForValue().get(key);
        Shop shop = null;
        if(shopJson==null)
        {
            log.info("店铺信息不存在，查询数据库");
            shop = this.getById(id);
            if(shop==null)
            {
                log.info("店铺不存在");
                redisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
                return null;
            }
            setWithLogicalExpire(key, shop, 30*60L);
            return shop;
        }
        if(shopJson!=null&&shopJson.length()>0) {
            log.info("店铺信息已存在，开始解析数据和逻辑时间");
            RedisData redisData = JSON.parseObject(shopJson, RedisData.class);
            shop = JSON.parseObject(redisData.getData().toString(), Shop.class);
            LocalDateTime expireTime = redisData.getExpireTime();
            if (expireTime.isAfter(LocalDateTime.now())){
                log.info("缓存未过期，直接返回");
                return shop;
            }
            RLock lk = redissonClient.getLock(lock);
            boolean isLock = lk.tryLock();
            if(isLock) {
                log.info("获取锁成功，开始重建缓存");
                try {
                    //二次检查
                    shopJson = redisTemplate.opsForValue().get(key);
                    if (shopJson != null && shopJson.length() > 0)
                    {
                        redisData = JSON.parseObject(shopJson, RedisData.class);
                        shop = JSON.parseObject(redisData.getData().toString(), Shop.class);
                        if (expireTime.isAfter(LocalDateTime.now())){
                            log.info("缓存未过期，直接返回");
                            return shop;
                        }
                    }
                    shop = this.getById(id);
                    if(shop==null)
                    {
                        log.info("店铺不存在");
                        redisTemplate.opsForValue().set(key, "", 2L, TimeUnit.MINUTES);
                        return null;
                    }
                    setWithLogicalExpire(key, shop, 30*60L);
                } finally {
                    lk.unlock();
                }
            }
        }
        return shop;
    }
    public void setWithLogicalExpire(String key, Object data, Long expireSeconds) {
        RedisData redisData = new RedisData();
        redisData.setData(data);
        // 设置逻辑过期时间 = 当前时间 + 过期秒数
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));

        // Redis 层面永不过期
        redisTemplate.opsForValue().set(key, JSON.toJSONString(redisData));
    }
}
