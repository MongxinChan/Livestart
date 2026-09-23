package com.mongxin.livestart.merchant.admin.service.log;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 从网关认证后注入的请求头中读取当前操作人。
 */
@Component
public class RequestOperatorContext {

    private static final String SYSTEM_OPERATOR = "system";
    private static final String INTERNAL_TOKEN_HEADER = "X-Livestart-Internal-Token";

    @Value("${livestart.merchant-admin.internal-token:change-me}")
    private String internalToken;

    public String getOperatorId() {
        return firstNotBlank(header("userId"), SYSTEM_OPERATOR);
    }

    public String getOperatorName() {
        return firstNotBlank(header("realName"), header("username"), getOperatorId());
    }

    public Integer getUserType() {
        String value = header("userType");
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public boolean isInternalCall() {
        String requestToken = header(INTERNAL_TOKEN_HEADER);
        return StrUtil.isNotBlank(internalToken) && internalToken.equals(requestToken);
    }

    private String header(String name) {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return request.getHeader(name);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return SYSTEM_OPERATOR;
    }
}
