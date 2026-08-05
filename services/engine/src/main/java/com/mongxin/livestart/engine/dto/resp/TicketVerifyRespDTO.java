package com.mongxin.livestart.engine.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 现场验票结果。
 */
@Data
@Schema(description = "现场验票结果")
public class TicketVerifyRespDTO {

    @Schema(description = "订单流水号")
    private String orderNo;

    @Schema(description = "电子票核销码")
    private String checkCode;

    @Schema(description = "演出ID")
    private Long eventId;

    @Schema(description = "票档ID")
    private Long skuId;

    @Schema(description = "观演人ID")
    private Long visitorId;

    @Schema(description = "核验状态")
    private String status;

    @Schema(description = "核验时间")
    private Date checkedAt;
}