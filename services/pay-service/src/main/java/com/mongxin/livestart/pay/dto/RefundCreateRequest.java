package com.mongxin.livestart.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建退款单并调用支付宝退款接口的请求
 */
@Data
public class RefundCreateRequest {

    /**
     * 待退款的业务订单号
     */
    @NotBlank
    private String orderNo;

    /**
     * 用户填写的退款原因，可选
     */
    private String reason;

    /**
     * 本次退款金额
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    @Digits(integer = 8, fraction = 2, message = "退款金额最多保留两位小数")
    private BigDecimal refundAmount;
}
