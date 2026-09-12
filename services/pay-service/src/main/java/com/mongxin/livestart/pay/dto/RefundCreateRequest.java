package com.mongxin.livestart.pay.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RefundCreateRequest {
    @NotBlank
    private String orderNo;
    private String reason;
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0")
    @Digits(integer = 8, fraction = 2, message = "退款金额最多保留两位小数")
    private BigDecimal refundAmount;
}
