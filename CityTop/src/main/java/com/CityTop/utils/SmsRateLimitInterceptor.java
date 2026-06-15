package com.CityTop.utils;

import cn.hutool.extra.servlet.ServletUtil;
import com.CityTop.dto.Result;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;

import static com.CityTop.utils.RedisConstants.*;
import static com.CityTop.utils.RegexUtils.isPhoneInvalid;

/**
 * 短信验证码接口限流拦截器（只挂在 /user/code）
 * 两层防刷：
 *   1. 同 IP 每分钟最多 {@link RedisConstants#RATE_LIMIT_SMS_IP_MAX} 次   —— 防一个 IP 刷不同号
 *   2. 同手机号每分钟最多 {@link RedisConstants#RATE_LIMIT_SMS_PHONE_MAX} 次 —— 防多个 IP 轰同一个号
 */
@Component
@Slf4j
public class SmsRateLimitInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT;
    static {
        RATE_LIMIT_SCRIPT = new DefaultRedisScript<>();
        RATE_LIMIT_SCRIPT.setLocation(new ClassPathResource("rate_limit.lua"));
        RATE_LIMIT_SCRIPT.setResultType(Long.class);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String phone = request.getParameter("phone");
        // 1. 手机号格式校验（先校验再计数，避免脏 key 污染 Redis）
        if (isPhoneInvalid(phone)) {
            return reject(response, "手机号格式错误");
        }
        // 2. 同 IP 限流：每分钟最多 RATE_LIMIT_SMS_IP_MAX 次
        String ip = ServletUtil.getClientIP(request);
        if (!allow(RATE_LIMIT_SMS_IP_KEY + ip, RATE_LIMIT_SMS_IP_MAX)) {
            log.warn("短信接口 IP 限流触发: ip={}, phone={}", ip, phone);
            return reject(response, "操作过于频繁，请稍后再试");
        }
        // 3. 同手机号限流：每分钟最多 RATE_LIMIT_SMS_PHONE_MAX 次
        if (!allow(RATE_LIMIT_SMS_PHONE_KEY + phone, RATE_LIMIT_SMS_PHONE_MAX)) {
            log.warn("短信接口手机号限流触发: phone={}", phone);
            return reject(response, "验证码发送过于频繁，请1分钟后再试");
        }
        return true;
    }

    /**
     * 执行限流脚本。
     * @return true=放行, false=超限拒绝
     */
    private boolean allow(String key, int max) {
        Long result = redisTemplate.execute(
                RATE_LIMIT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(max),
                String.valueOf(RATE_LIMIT_SMS_WINDOW)
        );
        return result != null && result == 1L;
    }

    /**
     * 拒绝请求：写回与全局一致的 Result JSON（HTTP 200 + success=false），前端可直接弹出 errorMsg。
     */
    private boolean reject(HttpServletResponse response, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(JSON.toJSONString(Result.fail(msg)));
        return false;
    }
}
