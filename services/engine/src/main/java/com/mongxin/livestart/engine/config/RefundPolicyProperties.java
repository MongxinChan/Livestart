package com.mongxin.livestart.engine.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Data
@Configuration
@ConfigurationProperties(prefix = "livestart.engine.refund-policy")
public class RefundPolicyProperties {
    private int defaultTier1DeadlineHours = 48;
    private int defaultTier2DeadlineHours = 24;
    private BigDecimal defaultTier2RefundFeeRate = new BigDecimal("0.20");
}
