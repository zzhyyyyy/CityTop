package com.CityTop.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "citytop.xxl-job", name = "enabled", havingValue = "true")
public class XxlJobConfig {

    @Bean(initMethod = "start", destroyMethod = "destroy")
    public XxlJobSpringExecutor xxlJobExecutor(
            @Value("${citytop.xxl-job.admin-addresses}") String adminAddresses,
            @Value("${citytop.xxl-job.appname}") String appName,
            @Value("${citytop.xxl-job.ip:}") String ip,
            @Value("${citytop.xxl-job.port}") int port,
            @Value("${citytop.xxl-job.access-token:}") String accessToken,
            @Value("${citytop.xxl-job.log-path}") String logPath,
            @Value("${citytop.xxl-job.log-retention-days}") int logRetentionDays) {
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appName);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
