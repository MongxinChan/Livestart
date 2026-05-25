package com.mongxin.livestart.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Livestart 网关服务
 * 负责：请求路由转发、登录态 Token 校验、用户信息 Header 注入、请求日志打印
 *
 * @author Mongxin
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
