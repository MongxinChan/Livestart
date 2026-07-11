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

    /**
     * 关联演出ID
     */
    @Schema(description = "关联演出ID", example = "1", required = true)
    private Long eventId;

    /**
     * 票种名称
     */
    @Schema(description = "票种名称", example = "680元档", required = true)
    private String title;

    /**
     * 原价
     */
    @Schema(description = "原价", example = "680.00")
    private BigDecimal originalPrice;

    /**
     * 售价
     */
    @Schema(description = "售价", example = "580.00", required = true)
    private BigDecimal sellingPrice;

    /**
     * 总库存
     */
    @Schema(description = "总库存", example = "500", required = true)
    private Integer totalStock;

    /**
     * 一开释放库存
     */
    @Schema(description = "一开释放库存", example = "300")
    private Integer stage1Stock;

    /**
     * 二开释放库存
     */
    @Schema(description = "二开释放库存", example = "200")
    private Integer stage2Stock;

    /**
     * 单人限购数量
     */
    @Schema(description = "单人限购数量", example = "4")
    private Integer limitNum;
}
