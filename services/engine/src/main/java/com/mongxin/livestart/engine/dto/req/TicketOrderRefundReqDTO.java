package com.mongxin.livestart.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 退票申请请求 DTO
 */
@Data
@Schema(description = "退票申请请求")
public class TicketOrderRefundReqDTO {

    /**
     * 订单流水号
     */
    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单流水号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;

    /**
     * 退票原因（可选）
     */
    @Schema(description = "退票原因")
    private String reason;
}
