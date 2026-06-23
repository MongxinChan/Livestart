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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ticket_reminder")
public class TicketReminderDO {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 演出 ID */
    private Long eventId;

    /** 用户 ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 手机号 */
    private String phone;

    /** 演出标题 */
    private String eventTitle;

    /** 开票阶段：1-一开 2-二开 */
    private Integer ticketStage;

    /** 开售时间 */
    private Date saleStartTime;

    /** 提醒触发时间（开售前 N 分钟） */
    private Date remindTime;

    /** 提醒状态，见 TicketReminderStatusEnum */
    private Integer status;

    /** XXL-JOB 任务 ID */
    private Integer xxlJobId;

    /** 提醒消息内容 */
    private String reminderMessage;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    private Integer delFlag;
}
