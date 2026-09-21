package com.mongxin.livestart.pay.remote.dto;

import lombok.Data;

import java.math.BigDecimal;

/** engine 提供给支付服务的可支付订单摘要。 */
@Data
public class PayableOrderDTO {
    /** 业务订单号。 */
    private String orderNo;
    /** 下单用户 ID。 */
    private Long userId;
    /** 订单应付总金额。 */
    private BigDecimal totalAmount;
    /** 订单状态，0 表示待支付。 */
    private Integer status;
}
