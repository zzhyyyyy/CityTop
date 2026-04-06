package com.CityTop.service.impl;

import com.alibaba.fastjson.JSON;
import com.CityTop.entity.ShopType;
import com.CityTop.mapper.ShopTypeMapper;
import com.CityTop.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

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
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public List<ShopType> queryByList() {
        log.info("查询所有店铺分类");
        String key = "shop:type:list";
        List<String> shopTypeList = redisTemplate.opsForList().range(key, 0, -1);
        List<ShopType> result;
        if(shopTypeList!=null&&shopTypeList.size()>0)
        {
            result = shopTypeList.stream()
                    .map(shopType -> JSON.parseObject(shopType, ShopType.class))
                    .collect(Collectors.toList());
            log.info("店铺分类已存在，直接返回");
            return result;
        }
        result = this.query().orderByAsc("sort").list();
        if(result==null)
        {
            log.info("店铺分类不存在");
            return null;
        }
        // 将 List<ShopType> 转为 List<String> JSON 字符串列表
        List<String> jsonList = result.stream()
                .map(JSON::toJSONString)  // 或 shopType -> JSON.toJSONString(shopType)
                .collect(Collectors.toList());
        // 存入 Redis
        redisTemplate.opsForList().rightPushAll(key, jsonList);
        redisTemplate.expire(key, 30L, java.util.concurrent.TimeUnit.MINUTES);
        log.info("店铺分类不存在，写入缓存");
        return result;
    }
}
