package com.mongxin.livestart.engine.remote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** engine 调用 pay-service 创建退款的请求。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundCreateRequestDTO {
    /** 待退款的业务订单号。 */
    private String orderNo;
    /** 用户填写的退款原因。 */
    private String reason;
    /** 本次退款金额。 */
    private BigDecimal refundAmount;
}
