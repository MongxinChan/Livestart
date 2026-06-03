package com.mongxin.livestart.framework.config;

import com.mongxin.livestart.framework.web.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Web 组件自动装配
 */
public class WebAutoConfiguration {

    /**
     * 构建全局异常拦截器组件 Bean
     */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler defaultGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}

