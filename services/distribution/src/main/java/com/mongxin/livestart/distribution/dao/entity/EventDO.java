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
 * 分销服务演出持久化实体（DO）。
 *
 * <p>对应公共库 {@code t_event}，保存商户演出发布后的分销副本、来源演出关联及售票状态。
 * {@code sourceEventId} 指向商户原始演出，{@code id} 仍是当前分销副本的全局主键。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_event")
public class EventDO {

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 商户后台的原始演出 ID，用于关联分销演出副本与来源演出。
     */
    private Long sourceEventId;

    /**
     * 演出标题。
     */
    private String title;

    /**
     * 主演艺人用户 ID。
     */
    private Long artistId;

    /**
     * 主演艺人名称。
     */
    private String artistName;

    /**
     * 关联场馆 ID。
     */
    private Long venueId;

    /**
     * 演出类型：0 表示 Livehouse 站票，1 表示演唱会选座。
     */
    private Integer eventType;

    /**
     * 公共演出表使用的标准开始时间。
     */
    private Date startTime;

    /**
     * 演出开始时间，兼容分销接口字段。
     */
    private Date eventTime;

    /**
     * 最早开售时间。
     */
    private Date saleStartTime;

    /**
     * 演出状态：0 下架，1 待售，2 在售，3 售罄。
     */
    private Integer status;

    /**
     * 定时开售对应的 XXL-JOB 任务 ID。
     */
    private Integer xxlJobId;

    /**
     * 创建时间。
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间。
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 逻辑删除标记：0 未删除，1 已删除。
     */
    @TableLogic
    private Integer delFlag;
}
