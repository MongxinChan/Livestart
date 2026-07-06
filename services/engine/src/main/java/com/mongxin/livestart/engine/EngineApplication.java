package com.mongxin.livestart.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Livestart 购票引擎服务启动类
 */
@SpringBootApplication
@MapperScan("com.mongxin.livestart.engine.dao.mapper")
@EnableFeignClients("com.mongxin.livestart.engine.remote")
@EnableScheduling
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }
}
