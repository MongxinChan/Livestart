package com.mongxin.livestart.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 票种增发库存接口请求参数
 */
@Data
@Schema(description = "票种增发库存参数")
public class TicketSkuIncreaseStockReqDTO {

    @Schema(description = "票种ID", required = true)
    private Long skuId;

    @Schema(description = "增发数量", example = "100", required = true)
    private Integer count;
}
