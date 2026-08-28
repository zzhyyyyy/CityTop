package com.CityTop.config;

import com.CityTop.utils.SmsRateLimitInterceptor;
import com.CityTop.utils.interceptor;
import com.CityTop.utils.interceptorAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

@Configuration
@Slf4j
public class WebMvcConfiguration  extends WebMvcConfigurationSupport {
    @Autowired
    private interceptor interceptor;
    @Autowired
    private interceptorAll interceptorAll;
    @Autowired
    private SmsRateLimitInterceptor smsRateLimitInterceptor;
    protected void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器");
        // 短信验证码接口限流：同IP每分钟≤10次、同手机号每分钟≤1次，最先执行
        registry.addInterceptor(smsRateLimitInterceptor)
                .addPathPatterns("/user/code").order(-1);
        registry.addInterceptor(interceptor)
                .excludePathPatterns("/user/code")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/shop/**")
                .excludePathPatterns("/shop-type/**")
                .excludePathPatterns("upload/**").order(1);
        registry.addInterceptor(interceptorAll)
                .addPathPatterns("/**").order(0);
    }
}
