package com.CityTop;

import com.CityTop.entity.Shop;
import com.CityTop.service.IShopService;
import com.CityTop.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Autowired
    private RedisIdWorker redisIdWorker;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private IShopService shopService;
    @Test
    void testRedisIdWorker() throws InterruptedException {
        for (int i = 0; i < 1000; i++) {
            long id = redisIdWorker.nextId("order");
            System.out.println("id = " + id);
        }
    }

    @Test
    void addShopData(){
        //先获取了所有的店铺信息
        List<Shop> list = shopService.list();
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for (Map.Entry<Long, List<Shop>> entry : map.entrySet()) {
            //分组完成后，对分组进行遍历
            Long typeId = entry.getKey();
            String key = "shop:geo:".concat(typeId.toString());
            List<Shop> value = entry.getValue();
            for(Shop shop : value){
                //写入redis
                stringRedisTemplate.opsForGeo().add(key, new Point(shop.getX(), shop.getY()), shop.getId().toString());
            }
        }
    }

}
