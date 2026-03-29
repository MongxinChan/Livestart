package com.mongxin.livestart.admin.common.serialize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * 手机号脱敏反序列化
 *
 * @author Mongxin
 */
public class PhoneDesensitizationSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String phone, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        String phoneDesensitization = phone;
        try {
            // 获取当前请求的 Spring 容器上下文
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                // 直接获取请求 URI，若是获取真实资料的高优白名单接口，则不脱敏
                String requestURI = attributes.getRequest().getRequestURI();
                if (!requestURI.contains("/actual/")) {
                    phoneDesensitization = DesensitizedUtil.mobilePhone(phone);
                }
            } else {
                // 若获取不到上下文（如非 Web 环境或异步线程），默认安全起见强制脱敏
                phoneDesensitization = DesensitizedUtil.mobilePhone(phone);
            }
        } catch (Exception e) {
            // 兜底方案：发生任何异常时，安全第一，执行脱敏
            phoneDesensitization = DesensitizedUtil.mobilePhone(phone);
        }
        jsonGenerator.writeString(phoneDesensitization);
    }
}