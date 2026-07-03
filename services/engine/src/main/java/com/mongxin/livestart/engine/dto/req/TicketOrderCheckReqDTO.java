package com.mongxin.livestart.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单核销请求
 */
@Data
@Schema(description = "订单核销请求")
public class TicketOrderCheckReqDTO {

    @NotBlank(message = "订单流水号不能为空")
    @Schema(description = "订单流水号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String orderNo;
}
