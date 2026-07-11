package com.mongxin.livestart.settlement.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 演出结算单详情响应 DTO
 */
@Data
public class SettlementRespDTO {

    /**
     * 结算记录 ID
     */
    private Long id;

    /**
     * 演出活动 ID
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
     * 本场演出总出票数
     */
    private Integer totalTickets;

    /**
     * 总销售票房金额
     */
    private BigDecimal totalSalesAmount;

    /**
     * 佣金比例
     */
    private BigDecimal commissionRate;

    /**
     * 平台佣金金额
     */
    private BigDecimal commissionAmount;

    /**
     * 主办方应结净额
     */
    private BigDecimal settlementAmount;

    /**
     * 结算状态 0:未结算 1:已结算 2:结算异常
     */
    private Integer status;

    /**
     * 结算异常信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
