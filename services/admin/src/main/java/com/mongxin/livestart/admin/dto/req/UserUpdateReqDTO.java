package com.mongxin.livestart.admin.dto.req;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;

/**
 * @author Mongxin
 */
@Data
public class UserUpdateReqDTO {

    /**
     * 用户昵称 (解禁，此时作为允许被修改的普通对象值)
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
     * 手机号 (升格为定位标识定海神针，必填！)
     */
    @NotBlank(message = "用来定位修改目标的手机号不能为空")
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