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

    /**
     * 订单流水号
     */
    @Schema(description = "订单流水号")
    private String orderNo;

    /**
     * 电子票核销码
     */
    @Schema(description = "电子票核销码")
    private String checkCode;

    /**
     * 演出ID
     */
    @Schema(description = "演出ID")
    private Long eventId;

    /**
     * 票档ID
     */
    @Schema(description = "票档ID")
    private Long skuId;

    /**
     * 观演人ID
     */
    @Schema(description = "观演人ID")
    private Long visitorId;

    /**
     * 核验状态
     */
    @Schema(description = "核验状态")
    private String status;

    /**
     * 核验时间
     */
    @Schema(description = "核验时间")
    private Date checkedAt;

    /**
     * 核销操作人的后台用户 ID。
     */
    @Schema(description = "核销操作人的后台用户 ID")
    private Long checkedBy;
}
