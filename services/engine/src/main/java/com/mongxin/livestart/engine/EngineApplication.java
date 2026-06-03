package com.mongxin.livestart.engine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Livestart 购票引擎服务启动类
 */
@SpringBootApplication
@MapperScan("com.mongxin.livestart.engine.dao.mapper")
@EnableFeignClients("com.mongxin.livestart.engine.remote")
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }
}
