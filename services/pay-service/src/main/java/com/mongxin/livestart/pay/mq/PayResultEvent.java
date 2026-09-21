package com.mongxin.livestart.pay.mq;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付成功事件，写入 Outbox 后由 RocketMQ 投递给 engine
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayResultEvent {

    /**
     * 事件唯一 ID
     */
    private String eventId;

    /**
     * 平台支付流水号
     */
    private String paySn;

    /**
     * 业务订单号
     */
    private String orderNo;

    /**
     * 支付用户 ID
     */
    private Long userId;

    /**
     * 支付宝交易号
     */
    private String tradeNo;

    /**
     * 支付宝确认的实际支付金额
     */
    private BigDecimal payAmount;

    /**
     * 支付完成时间
     */
    private Date paidAt;
}
