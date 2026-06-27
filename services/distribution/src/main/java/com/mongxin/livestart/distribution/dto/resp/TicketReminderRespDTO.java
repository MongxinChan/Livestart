package com.mongxin.livestart.distribution.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 开售提醒记录响应传输对象 DTO。
 * 用于客户端展示用户已订阅的开售提醒记录。
 */
@Data
public class TicketReminderRespDTO {

    /**
     * 提醒记录主键 ID
     */
    private Long id;

    /**
     * 关联的演出 ID
     */
    private Long eventId;

    /**
     * 演出标题
     */
    private String eventTitle;

    /**
     * 关联的开售阶段 ID
     */
    private Long stageId;

    /**
     * 开售阶段序号（1:一开，2:二开，3:三开...）
     */
    private Integer stageNo;

    /**
     * 开售阶段名称
     */
    private String stageName;

    /**
     * 兼容字段：门票状态阶段/原提醒状态类型（如 1:一开，2:二开）
     */
    private Integer ticketStage;

    /**
     * 提醒短信任务状态（0:待发送，1:发送中，2:发送成功，3:发送失败）
     */
    private Integer status;

    /**
     * 提醒状态的中文描述
     */
    private String statusDesc;

    /**
     * 演出开售时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    /**
     * 预设发送提醒短信的时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date remindTime;

    /**
     * 提醒短信的具体文本内容
     */
    private String reminderMessage;
}
