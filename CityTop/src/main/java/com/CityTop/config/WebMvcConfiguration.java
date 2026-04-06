package com.CityTop.config;

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
    protected void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        log.info("开始注册自定义拦截器");
        registry.addInterceptor(interceptor)
                .excludePathPatterns("/user/code")
                .excludePathPatterns("/user/login")
                .excludePathPatterns("/shop/**")
                .excludePathPatterns("/shop-type/**")
                .excludePathPatterns("upload/**")
                .excludePathPatterns("/voucher/**").order(1);
        registry.addInterceptor(interceptorAll)
                .addPathPatterns("/**").order(0);
    }
}
