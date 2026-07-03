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
 * 演出售票阶段实体，映射到演出售票阶段表 {@code t_event_sale_stage}。
 * 用于定义演出的多阶段售票配置（如：一开、二开、三开等不同开售时间和状态）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_event_sale_stage")
public class EventSaleStageDO {

    /**
     * 阶段主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联演出ID
     */
    private Long eventId;

    /**
     * 阶段序号（例如：1-一开，2-二开，3-三开）
     */
    private Integer stageNo;

    /**
     * 阶段名称（如 "首发开售"、"二次开售"）
     */
    private String stageName;

    /**
     * 该阶段统一开售时间
     */
    private Date saleStartTime;

    /**
     * 阶段状态（0:待开售，1:已开售，2:已完成，3:已取消）
     */
    private Integer status;

    /**
     * 绑定的XXL-JOB任务ID，用于到达开售时间时自动开启该阶段
     */
    private Integer xxlJobId;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 逻辑删除标识（0:未删除，1:已删除）
     */
    @TableLogic
    private Integer delFlag;
}
