package com.mongxin.livestart.admin.config;

import com.mongxin.livestart.admin.common.biz.user.UserContext;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenFeignConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            template.header("username", UserContext.getUsername());
            template.header("userId", UserContext.getUserId());
            template.header("realName", UserContext.getRealName());
        };
    }
}