package com.mongxin.livestart.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 常用观演人持久层实体
 * 对应表：t_user_visitor
 */
@Data
@TableName("t_user_visitor")
public class UserVisitorDO {

    /**
     * ID（AUTO_INCREMENT）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属用户ID（关联 t_user.id）
     */
    private Long userId;

    /**
     * 观演人真实姓名
     */
    private String realName;

    /**
     * 证件类型 1:身份证 2:护照 3:港澳通行证 4:台胞证
     */
    private Integer cardType;

    /**
     * 证件号码（AES 加密存储）
     */
    private String cardNo;

    /**
     * 证件号哈希值（SHA-256，用于判重，可索引）
     */
    private String cardNoHash;

    /**
     * 观演人手机号（选填，部分演出需要通知到人）
     */
    private String mobile;

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
     * 逻辑删除 0:未删 1:已删
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;
}
