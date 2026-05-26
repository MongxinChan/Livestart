package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 歌迷特权特价门票高并发抢购/秒杀请求 DTO
 */
@Data
@Schema(description = "门票秒杀抢票请求")
public class TicketGrabReqDTO {

    @Schema(description = "抢购门票票档 SkuID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "秒杀门票 SkuID 不能为空")
    private Long ticketSkuId;

    @Schema(description = "分销来源艺人专属宣发推广码 (静默追踪绑定)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String artistPromoCode;
}
