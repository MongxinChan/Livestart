package com.mongxin.livestart.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付回调请求 DTO（由支付网关回调，通知引擎出票）
 */
@Data
@Schema(description = "支付回调请求")
public class TicketOrderPayCallbackReqDTO {

    /**
     * 订单流水号
     */
    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单流水号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    /**
     * 第三方支付流水号
     */
    @NotBlank(message = "支付流水号不能为空")
    @Schema(description = "第三方支付流水号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tradeNo;

    /**
     * 实付金额
     */
    @NotNull(message = "实付金额不能为空")
    @Schema(description = "实付金额", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal payAmount;
}
