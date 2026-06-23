package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * merchant-admin 票档查询响应（Feign 接收用）
 */
@Data
public class MerchantTicketSkuRespDTO {

    private Long id;
    private Long eventId;
    private String title;
    private BigDecimal originalPrice;
    private BigDecimal sellingPrice;
    private Integer totalStock;
    private Integer stage1Stock;
    private Integer stage2Stock;
    private Integer remainingStock;
    private Integer limitNum;
}
