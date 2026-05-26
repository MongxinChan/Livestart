package com.mongxin.livestart.distribution.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / Knife4j 接口文档配置
 */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI livestartDistributionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Livestart 演唱会门票分销分发及抢票服务 API")
                        .description("负责门票票档发布、歌迷高并发秒杀特价票、批量推送赠票，以及艺人专属宣发渠道提成个税核算等业务服务")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Livestart Team")
                                .email("dev@mongxin.com")));
    }
}
