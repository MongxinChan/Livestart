package com.mongxin.livestart.admin.dto.req;

import lombok.Data;

import java.util.Date;

/**
 * @author Mongxin
 */
@Data
public class UserUpdateReqDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

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

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 性别 0:保密 1:男 2:女
     */
    private Integer gender;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 个性签名
     */
    private String signature;
}