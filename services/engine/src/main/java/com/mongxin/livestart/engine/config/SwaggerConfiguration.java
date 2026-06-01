package com.mongxin.livestart.engine.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / Knife4j 配置
 */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI livestartEngineOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Livestart 购票引擎服务 API")
                        .description("负责用户购票下单、支付回调、取消与退票的核心业务服务")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Livestart Team")
                                .email("dev@mongxin.com")));
    }
}
