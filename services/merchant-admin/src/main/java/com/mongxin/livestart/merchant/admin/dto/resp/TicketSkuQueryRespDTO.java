package com.mongxin.livestart.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 票种详情查询接口返回参数
 */
@Data
@Schema(description = "票种详情查询返回实体")
public class TicketSkuQueryRespDTO {

    @Schema(description = "票种ID")
    private Long id;

    @Schema(description = "关联演出ID")
    private Long eventId;

    @Schema(description = "票种名称")
    private String title;

    @Schema(description = "原价")
    private BigDecimal originalPrice;

    @Schema(description = "售价")
    private BigDecimal sellingPrice;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "当前剩余库存")
    private Integer remainingStock;

    @Schema(description = "单人限购数量")
    private Integer limitNum;

    @Schema(description = "乐观锁版本号")
    private Integer version;
}
