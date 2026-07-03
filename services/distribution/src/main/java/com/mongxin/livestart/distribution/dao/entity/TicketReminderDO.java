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
 * 门票开售提醒实体，映射到开售提醒记录表 {@code t_ticket_reminder}。
 * 当用户订阅某场演出某阶段的开售提醒时，系统将生成提醒记录，并在开售前通过 XXL-JOB 触发提醒（如发送短信/通知）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_reminder")
public class TicketReminderDO {

    /**
     * 提醒记录主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联演出ID
     */
    private Long eventId;

    /**
     * 关联用户ID
     */
    private Long userId;

    /**
     * 接收提醒的用户名
     */
    private String username;

    /**
     * 接收提醒的手机号
     */
    private String phone;

    /**
     * 演出标题（冗余字段，便于组装提醒短信内容）
     */
    private String eventTitle;

    /**
     * 关联的开售阶段ID（支持多开售阶段提醒，如一开、二开）
     */
    private Long stageId;

    /**
     * 关联的开售阶段序号（1:一开，2:二开，3:三开...）
     */
    private Integer stageNo;

    /**
     * 关联的开售阶段名称（如 "首发开售"、"二次开售"）
     */
    private String stageName;

    /**
     * 兼容历史版本的门票状态阶段/原提醒状态类型（如 1:一开，2:二开）
     */
    private Integer ticketStage;

    /**
     * 对应阶段开售时间，用于计算提醒执行时间
     */
    private Date saleStartTime;

    /**
     * 预设发送提醒短信的时间（通常比开售时间提前 10-15 分钟）
     */
    private Date remindTime;

    /**
     * 提醒状态（0:待发送，1:发送中，2:发送成功，3:发送失败）
     */
    private Integer status;

    /**
     * 绑定的发送提醒的XXL-JOB任务ID
     */
    private Integer xxlJobId;

    /**
     * 提醒短信或消息的具体内容
     */
    private String reminderMessage;

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
