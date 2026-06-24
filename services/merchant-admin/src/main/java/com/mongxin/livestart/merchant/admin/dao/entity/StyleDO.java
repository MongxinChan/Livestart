package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 音乐风格持久层实体
 * 对应表：t_style
 */
@Data
@TableName("t_style")
public class StyleDO {

    /**
     * 分布式主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 风格名称
     */
    private String name;

    /**
     * 风格代码 (如: ROCK)
     */
    private String code;

    /**
     * 风格描述
     */
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;
}
