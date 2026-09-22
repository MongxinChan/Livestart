package com.mongxin.livestart.admin.common.serialize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

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
        String idCardDesensitization = DesensitizedUtil.idCardNum(idCard, 4, 4);
        jsonGenerator.writeString(idCardDesensitization);
    }
}
