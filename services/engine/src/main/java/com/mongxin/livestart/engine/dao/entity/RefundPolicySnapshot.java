package com.mongxin.livestart.engine.dao.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单退票判定所需的演出时间与策略快照。
 */
@Data
public class RefundPolicySnapshot {

    /**
     * 演出时间
     */
    private Date eventStartTime;

    /**
     * 是否允许退款
     */
    private Integer allowRefund;

    /**
     * 第一阶段退款时间
     * 如：演出前 10 天
     */
    private Integer tier1DeadlineHours;

    /**
     * 第二阶段退款时间
     * 如：演出前 7 天
     */
    private Integer tier2DeadlineHours;

    /**
     * 第二阶段退款收取手续费
     * 如：在演出前 8 天退款收取的百分比
     */
    private BigDecimal tier2RefundFeeRate;
}
