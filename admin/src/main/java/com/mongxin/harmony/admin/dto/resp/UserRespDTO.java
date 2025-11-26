package com.mongxin.harmony.admin.dto.resp;

import com.mongxin.harmony.admin.common.serialize.PhoneDesensitizationSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

/**
 * 用户返回参数响应
 */
@Data
public class UserRespDTO {

    /**
     * ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 手机
     */
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    //读取出来字段，进行泛解析
    private String phone;

    /**
     * 邮箱
     */
    private String mail;
}
