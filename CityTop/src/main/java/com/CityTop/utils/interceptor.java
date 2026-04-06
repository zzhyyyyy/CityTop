package com.CityTop.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

import static com.CityTop.utils.RedisConstants.LOGIN_USER_KEY;

@Component
@Slf4j
public class interceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        /*
        let token = sessionStorage.getItem("token");
        axios.interceptors.request.use(
          config => {
            if(token) config.headers['authorization'] = token 这一段决定了request请求头里面是否有authorization字段 这样才能在request请求头里面有authorization字段
            if(token) config.headers['token'] = token;  这样才能在request请求头里面有token字段
            return config
          },
          error => {
            console.log(error)
            return Promise.reject(error)
          }
        )
         */
        String token = request.getHeader("token"); //取决于前端是怎么设置的
        if (token == null) {
            log.info("用户未登录");
            response.setStatus(401);
            return false;
        }
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(LOGIN_USER_KEY + token);
        if(entries.isEmpty()){
            response.setStatus(401);
            return false;
        }
        return true;
    }

}
