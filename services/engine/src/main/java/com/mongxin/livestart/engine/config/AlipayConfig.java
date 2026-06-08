package com.mongxin.livestart.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝沙箱环境配置映射类
 */
@Configuration
@ConfigurationProperties(prefix = "alipay")
@Data
public class AlipayConfig {

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 应用私钥，用于请求签名
     */
    private String privateKey;

    /**
     * 支付宝公钥，用于验证支付宝回调
     */
    private String publicKey;

    /**
     * 网关地址
     */
    private String gatewayUrl;

    /**
     * 异步通知回调地址
     */
    private String notifyUrl;

    /**
     * 支付成功后前端跳转地址
     */
    private String returnUrl;

    /**
     * 字符编码
     */
    private String charset = "UTF-8";

    /**
     * 签名类型
     */
    private String signType = "RSA2";
}
