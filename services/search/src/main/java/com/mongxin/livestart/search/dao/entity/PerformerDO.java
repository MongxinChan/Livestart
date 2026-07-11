package com.mongxin.livestart.search.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 艺人/乐队搜索引擎文档实体，对应表：t_performer
 */
@Data
@TableName("t_performer")
public class PerformerDO {

    /**
     * 艺人ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 艺人/乐队名称
     */
    private String name;

    /**
     * 音乐风格ID
     */
    private Long styleId;

    /**
     * 艺人头像
     */
    private String avatar;

    /**
     * 艺人详细介绍
     */
    private String bio;

    /**
     * 状态 1:正常 0:停演
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;
}
