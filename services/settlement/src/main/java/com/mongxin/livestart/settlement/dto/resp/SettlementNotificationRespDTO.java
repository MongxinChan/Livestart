package com.mongxin.livestart.settlement.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 结算通知/消息响应 DTO
 */
@Data
@Builder
public class SettlementNotificationRespDTO {

    /**
     * 通知消息唯一键 Key (Redis Stream Id / Kafka Offset 等)
     */
    private String notificationKey;

    /**
     * 关联结算记录 ID
     */
    private Long settlementId;

    /**
     * 演出活动ID
     */
    private Long eventId;

    /**
     * 演出标题
     */
    private String eventTitle;

    /**
     * 艺人名称
     */
    private String performerName;

    /**
     * 结算状态 0:未结算 1:已结算 2:结算异常
     */
    private Integer status;

    /**
     * 通知类型 (如: SETTLEMENT_COMPLETED)
     */
    private String type;

    /**
     * 通知类型中文标签
     */
    private String typeLabel;

    /**
     * 消息描述内容
     */
    private String description;

    /**
     * 商家应结净额
     */
    private BigDecimal settlementAmount;

    /**
     * 平台佣金抽成金额
     */
    private BigDecimal commissionAmount;

    /**
     * 门票销售总数
     */
    private Integer totalTickets;

    /**
     * 消息是否已读
     */
    private Boolean read;

    /**
     * 通知发送/更新时间
     */
    private Date updateTime;
}
