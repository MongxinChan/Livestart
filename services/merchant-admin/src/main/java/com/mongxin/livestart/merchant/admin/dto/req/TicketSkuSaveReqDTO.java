package com.mongxin.livestart.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 票种创建接口请求参数
 */
@Data
@Schema(description = "票种创建参数")
public class TicketSkuSaveReqDTO {

    @Schema(description = "关联演出ID", example = "1", required = true)
    private Long eventId;

    @Schema(description = "票种名称", example = "680元档", required = true)
    private String title;

    @Schema(description = "原价", example = "680.00")
    private BigDecimal originalPrice;

    @Schema(description = "售价", example = "580.00", required = true)
    private BigDecimal sellingPrice;

    @Schema(description = "总库存", example = "500", required = true)
    private Integer totalStock;

    @Schema(description = "单人限购数量", example = "4")
    private Integer limitNum;
}
