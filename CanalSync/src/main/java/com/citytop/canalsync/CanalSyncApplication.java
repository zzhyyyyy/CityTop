package com.citytop.canalsync;

import com.citytop.canalsync.config.CanalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(CanalProperties.class)
public class CanalSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(CanalSyncApplication.class, args);
    }
}
