package com.mongxin.harmony.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
//@EnableFeignClients("com.mongxin.harmony.admin.remote")
@MapperScan("com.mongxin.harmony.admin.dao.mapper")
public class LiveStartAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveStartAdminApplication.class, args);
    }

}
