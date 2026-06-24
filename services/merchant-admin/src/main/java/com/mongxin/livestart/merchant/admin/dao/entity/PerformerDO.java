package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 艺人持久层实体
 * 对应表：t_performer
 */
@Data
@TableName("t_performer")
public class PerformerDO {

    /**
     * 分布式主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 艺人/乐队名称
     */
    private String name;

    /**
     * 关联风格 ID
     */
    private Long styleId;

    /**
     * 艺人头像/Logo
     */
    private String avatar;

    /**
     * 简介
     */
    private String bio;

    /**
     * 状态：1 正常，0 停演
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
