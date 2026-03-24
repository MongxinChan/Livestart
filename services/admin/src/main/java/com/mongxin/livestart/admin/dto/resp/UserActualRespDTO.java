package com.mongxin.livestart.admin.dto.resp;

import lombok.Data;

/**
 * 用户返回参数响应
 */
@Data
public class UserActualRespDTO {

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
    private String phone;

    /**
     * 邮箱
     */
    private String mail;
}
