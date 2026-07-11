package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * merchant-admin 票档查询响应（Feign 接收用）
 */
@Data
public class MerchantTicketSkuRespDTO {

    /**
     * 票档ID
     */
    private Long id;

    /**
     * 关联演出活动ID
     */
    private Long eventId;

    /**
     * 票档规格名称
     */
    private String title;

    /**
     * 票面原价
     */
    private BigDecimal originalPrice;

    /**
     * 实际售价
     */
    private BigDecimal sellingPrice;

    /**
     * 总库存
     */
    private Integer totalStock;

    /**
     * 一开释放库存
     */
    private Integer stage1Stock;

    /**
     * 二开释放库存
     */
    private Integer stage2Stock;

    /**
     * 当前剩余可售库存
     */
    private Integer remainingStock;

    /**
     * 单个用户的限购张数
     */
    private Integer limitNum;
}
