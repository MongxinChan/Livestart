package com.mongxin.livestart.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 用户持久层实体
 */
@Data
@TableName("t_user")
public class UserDO {

    /**
     * ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 手机号 (唯一登录凭证)
     */
    private String phone;

    /**
     * 邮箱
     */
    private String mail;

    /*
     新增字段：实名认证相关 (票务系统核心)
     */

    /**
     * 真实姓名 (实名认证后写入)
     */
    private String realName;

    /**
     * 身份证号
     * 注意：此处必须存储加密后的字符串 (AES)，不可存明文
     */
    private String idCard;

    /**
     * 是否实名认证 0:否 1:是
     */
    private Integer isVerified;

    /*
     新增字段：社交与资料 (类秀动APP属性)
     */

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 性别 0:保密 1:男 2:女
     */
    private Integer gender;

    /**
     * 个性签名
     */
    private String signature;

    /*
     新增字段：账号状态与权限
     */

    /**
     * 账号状态 1:正常 0:禁用(封号)
     */
    private Integer status;

    /**
     * 用户类型 1:普通用户 2:主办方认证用户
     */
    private Integer userType;

    /*
     审计字段
     */

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 注销/删除时间
     */
    private Date deleteTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}