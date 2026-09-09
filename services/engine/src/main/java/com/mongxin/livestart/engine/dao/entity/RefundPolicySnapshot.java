package com.mongxin.livestart.engine.dao.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 订单退票判定所需的演出时间与策略快照。
 */
@Data
public class RefundPolicySnapshot {
    private Date eventStartTime;
    private Integer allowRefund;
    private Integer tier1DeadlineHours;
    private Integer tier2DeadlineHours;
    private BigDecimal tier2RefundFeeRate;
}
