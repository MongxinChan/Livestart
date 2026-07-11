package com.mongxin.livestart.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 票种详情查询响应。
 * 主要用于 merchant-admin 后台编辑页回显，也会被 engine 远程查询票种时复用字段语义。
 */
@Data
@Schema(description = "票种详情查询响应")
public class TicketSkuQueryRespDTO {

    /**
     * 票种ID
     */
    @Schema(description = "票种ID")
    private Long id;

    /**
     * 关联演出ID
     */
    @Schema(description = "关联演出ID")
    private Long eventId;

    /**
     * 票种名称
     */
    @Schema(description = "票种名称")
    private String title;

    /**
     * 票面原价
     */
    @Schema(description = "票面原价")
    private BigDecimal originalPrice;

    /**
     * 实际售卖价
     */
    @Schema(description = "实际售卖价")
    private BigDecimal sellingPrice;

    /**
     * 总库存
     */
    @Schema(description = "总库存")
    private Integer totalStock;

    /**
     * 一开释放库存
     */
    @Schema(description = "一开释放库存")
    private Integer stage1Stock;

    /**
     * 二开待释放库存
     */
    @Schema(description = "二开待释放库存")
    private Integer stage2Stock;

    /**
     * 当前剩余库存
     */
    @Schema(description = "当前剩余库存")
    private Integer remainingStock;

    /**
     * 单人限购数量
     */
    @Schema(description = "单人限购数量")
    private Integer limitNum;

    /**
     * 乐观锁版本号
     */
    @Schema(description = "乐观锁版本号")
    private Integer version;
}
