package com.CityTop.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.CityTop.dto.Result;
import com.CityTop.dto.UserDTO;
import com.CityTop.entity.User;
import com.CityTop.mapper.UserMapper;
import com.CityTop.service.IUserService;
import com.CityTop.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import static com.CityTop.utils.RegexUtils.isPhoneInvalid;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(isPhoneInvalid(phone)) {
            return Result.fail("手机号格式错误");
        }
        String code = RandomUtil.randomNumbers(6);
        log.info("发送验证码成功，验证码：{}",code);
        redisTemplate.opsForValue().set("user:code:" + phone, code,2, TimeUnit.MINUTES);
        return Result.ok();
    }

    @Override
    public Result getOtherInfoById(Long userId) {
        return Result.ok(getById(userId));
    }

    @Override
    public Result sign() {
        //1.获取当前用户
        UserDTO user = UserHolder.getUser();
        //2.获取当前时间
        LocalDate now = LocalDate.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern("yyyy:MM"));
        //3.获取当前天数
        int day = now.getDayOfMonth();
        //4.获取redis-key
        String key = "user:sign:" + user.getId() + ":" + keySuffix;
        //5.写入redis set bit user:sign:1 2023-01-01 1
        redisTemplate.opsForValue().setBit(key, day - 1, true);
        return Result.ok();
    }

    @Override
    public Result signCount() {
        String key = "user:sign:" + UserHolder.getUser().getId() + ":" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy:MM"));
        return Result.ok(redisTemplate.execute(
                (RedisCallback<Long>) conn -> conn.bitCount(key.getBytes()))
        );
    }

}
