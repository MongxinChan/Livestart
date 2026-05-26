package com.mongxin.livestart.merchant.admin.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI 接口文档配置
 */
@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI livestartMerchantAdminOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Livestart 商户后台管理系统")
                        .description("演出票务商户后台管理 API 接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Livestart")
                        )
                );
    }
}
