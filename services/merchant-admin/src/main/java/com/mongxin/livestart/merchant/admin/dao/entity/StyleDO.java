package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_style")
public class StyleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 风格名称 (如: 摇滚、民谣)
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
