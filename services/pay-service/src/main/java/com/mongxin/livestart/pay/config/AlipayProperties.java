package com.mongxin.livestart.pay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "livestart.pay.alipay")
public class AlipayProperties {
    private String appId;
    private String privateKey;
    private String publicKey;
    private String gatewayUrl;
    private String notifyUrl;
    private String returnUrl;
    private String charset = "UTF-8";
    private String signType = "RSA2";
}
