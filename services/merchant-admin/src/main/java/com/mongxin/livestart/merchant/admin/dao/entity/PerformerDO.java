package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_performer")
public class PerformerDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 艺人/乐队名称
     */
    private String name;

    /**
     * 关联风格ID
     */
    private Long styleId;

    /**
     * 艺人头像/Logo
     */
    private String avatar;

    /**
     * 介绍
     */
    private String bio;

    /**
     * 状态 1:正常 0:停演
     */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
