package com.mongxin.livestart.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mongxin.livestart.admin.common.serialize.PhoneDesensitizationSerializer;
import com.mongxin.livestart.admin.common.serialize.IdCardDesensitizationSerializer;
import lombok.Data;

import java.util.Date;

/**
 * 统一用户详情显示视图对象 (聚合 UserDO 与 UserProfileDO)
 * 
 * 架构命名解释与注释留痕：
 * 按照标准的业务架构分层，这个类包含了脱敏字段并且最终直接推向前端界面，它本质上是一个 VO（View Object）。
 * 但为了遵循当前工程中 "入参必须以 ReqDTO 结尾，出参必须以 RespDTO 结尾" 的严格对称性规范（Symmetry Naming），
 * 防止破坏项目接口层面整齐划一的画风，故在此将其定名为 UserRespDTO。
 * 虽然它叫 DTO，但在这个层面它 100% 承担了视图对象（VO）的聚合展示职能。
 *
 * @author Mongxin
 */
@Data
public class UserRespDTO {

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
    @JsonSerialize(using = IdCardDesensitizationSerializer.class)
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
