package com.mongxin.livestart.engine.remote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * merchant-admin 返回的票种详情。
 * engine 通过该 DTO 拉取权威票种数据，再转换为本地下单流程使用的 TicketSkuDO。
 */
@Data
@Schema(description = "merchant-admin 票种详情响应")
public class MerchantTicketSkuDetailRespDTO {

    @Schema(description = "票种ID")
    private Long id;

    @Schema(description = "关联演出ID")
    private Long eventId;

    @Schema(description = "票种名称")
    private String title;

    @Schema(description = "票面原价")
    private BigDecimal originalPrice;

    @Schema(description = "实际售卖价")
    private BigDecimal sellingPrice;

    @Schema(description = "总库存")
    private Integer totalStock;

    @Schema(description = "一开释放库存")
    private Integer stage1Stock;

    @Schema(description = "二开待释放库存")
    private Integer stage2Stock;

    @Schema(description = "当前剩余可售库存")
    private Integer remainingStock;

    @Schema(description = "单个用户的限购张数")
    private Integer limitNum;

    @Schema(description = "乐观锁版本号")
    private Integer version;
}
