package com.mongxin.livestart.merchant.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Livestart 后台商家管理系统自动装配启动类
 */
@SpringBootApplication(scanBasePackages = {
        "com.mongxin.livestart.merchant.admin",
        "com.mongxin.livestart.framework" // 扫描 framework 包中的配置和全局异常处理器
})
@MapperScan("com.mongxin.livestart.merchant.admin.dao.mapper")
public class LiveStartMerchantAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveStartMerchantAdminApplication.class, args);
    }
}
