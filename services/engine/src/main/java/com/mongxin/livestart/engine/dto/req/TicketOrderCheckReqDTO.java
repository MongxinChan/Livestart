package com.mongxin.livestart.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 电子票核验请求。
 */
@Data
@Schema(description = "电子票核验请求")
public class TicketOrderCheckReqDTO {

    /**
     * 电子票核销码，可手输或由扫码器读取。
     */
    @NotBlank(message = "电子票码不能为空")
    @Schema(description = "电子票核销码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String checkCode;
}
