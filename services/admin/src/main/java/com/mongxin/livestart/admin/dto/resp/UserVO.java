package com.mongxin.livestart.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongxin.livestart.admin.common.serialize.PhoneDesensitizationSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 统一用户详情显示视图对象 (聚合UserDO与UserProfileDO)
 * 
 * @author Mongxin
 */
@Data
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名 (昵称)
     */
    private String username;

    /**
     * 手机
     */
    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 是否已认证 0:否 1:是
     */
    private Integer isVerified;

    /**
     * 根据userType区分普通用户(1)、艺人(2)等
     */
    private Integer userType;

    /**
     * 账号状态 1:正常 0:禁用(封号)
     */
    private Integer status;

    /**
     * 社交邮箱
     */
    private String mail;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 个性签名
     */
    private String signature;

    /**
     * 性别 0:保密 1:男 2:女
     */
    private Integer gender;

    /**
     * 生日
     */
    private Date birthday;
}
