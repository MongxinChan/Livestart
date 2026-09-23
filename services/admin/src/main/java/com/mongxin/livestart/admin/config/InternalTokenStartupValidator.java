package com.mongxin.livestart.admin.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 生产环境禁止使用示例内部令牌。
 */
@Component
public class InternalTokenStartupValidator {

    @Value("${livestart.admin.internal-token:}")
    private String internalToken;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @PostConstruct
    public void validate() {
        if (!isProductionProfile()) {
            return;
        }
        if (internalToken == null || internalToken.isBlank()
                || "change-me".equalsIgnoreCase(internalToken)
                || internalToken.length() < 16) {
            throw new IllegalStateException(
                    "生产环境禁止使用默认或过短的 LIVESTART_INTERNAL_TOKEN，请通过环境变量注入至少 16 位随机令牌");
        }
    }

    private boolean isProductionProfile() {
        if (activeProfiles == null) {
            return false;
        }
        for (String profile : activeProfiles.split(",")) {
            if ("prod".equalsIgnoreCase(profile.trim())
                    || "production".equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }
}
