package com.CityTop;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.CityTop.mapper")
@SpringBootApplication
public class CityTopApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityTopApplication.class, args);
    }

}
