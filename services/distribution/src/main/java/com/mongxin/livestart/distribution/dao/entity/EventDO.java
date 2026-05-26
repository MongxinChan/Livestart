package com.mongxin.livestart.distribution.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 演唱会演出实体 (存储于 ds_common.t_event)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_event")
public class EventDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 演出标题
     */
    private String title;

    /**
     * 主演艺人ID (分销主体)
     */
    private Long artistId;

    /**
     * 艺人姓名
     */
    private String artistName;

    /**
     * 演出时间
     */
    private Date eventTime;

    /**
     * 演出场馆地址
     */
    private String address;

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
     * 删除标记 0:未删除 1:已删除
     */
    @TableLogic
    private Integer delFlag;
}
