package com.mongxin.livestart.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 取消订单请求 DTO
 */
@Data
@Schema(description = "取消订单请求")
public class TicketOrderCancelReqDTO {

    /**
     * 订单流水号
     */
    @NotBlank(message = "订单号不能为空")
    @Schema(description = "订单流水号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;
}
