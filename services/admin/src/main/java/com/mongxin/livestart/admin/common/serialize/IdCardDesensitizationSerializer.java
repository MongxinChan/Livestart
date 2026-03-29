package com.mongxin.livestart.admin.common.serialize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * 身份证号防泄漏安全脱敏输出序列化器
 *
 * @author Mongxin
 */
public class IdCardDesensitizationSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String idCard, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        String idCardDesensitization = idCard;
        try {
            // 获取当前 Web 容器里的全套上下文去抓请求路径
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                // 判断访问的该接口是否是我们专门给内网微服务或管理员预留查看完整信息的白名单接口
                String requestURI = attributes.getRequest().getRequestURI();
                if (!requestURI.contains("/actual/")) {
                    idCardDesensitization = DesensitizedUtil.idCardNum(idCard, 4, 4);
                }
            } else {
                // 如果是异步线程或丢失 WebContext 情况下，强制启动脱敏保障安全
                idCardDesensitization = DesensitizedUtil.idCardNum(idCard, 4, 4);
            }
        } catch (Exception e) {
            // 代码运行时任意报错都进行强制脱敏兜底
            idCardDesensitization = DesensitizedUtil.idCardNum(idCard, 4, 4);
        }
        jsonGenerator.writeString(idCardDesensitization);
    }
}