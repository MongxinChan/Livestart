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
 * Distribution-side event aggregate mapped to {@code t_event}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_event")
public class EventDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** Event title. */
    private String title;

    /** Performer or artist id. */
    private Long artistId;

    /** Performer or artist name. */
    private String artistName;

    /** Related venue id. */
    private Long venueId;

    /** Event type: 0 livehouse, 1 concert. */
    private Integer eventType;

    /** Canonical event start time used by shared event schema. */
    private Date startTime;

    /** Event start time. */
    private Date eventTime;

    /** Ticket sale start time. */
    private Date saleStartTime;

    /** Event sale status. */
    private Integer status;

    /** Bound XXL-JOB id for scheduled sale release. */
    private Integer xxlJobId;

    /** Create time. */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** Update time. */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** Logical delete flag. */
    @TableLogic
    private Integer delFlag;
}
