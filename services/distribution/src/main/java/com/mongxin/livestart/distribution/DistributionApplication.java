package com.mongxin.livestart.distribution;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Livestart 演唱会门票发布、高并发抢票、大批量发票与艺人推广分成微服务启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.mongxin.livestart.distribution",
        "com.mongxin.livestart.framework"
})
@MapperScan("com.mongxin.livestart.distribution.dao.mapper")
@EnableFeignClients(basePackages = "com.mongxin.livestart.distribution.feign")
public class DistributionApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributionApplication.class, args);
    }
}
