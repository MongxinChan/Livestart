package com.mongxin.livestart.merchant.admin.remote.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家管理后台向分销系统发布演出信息时，演出票档的远程调用请求体 DTO。
 * 用于远程 Feign 接口间的数据传输。
 */
@Data
public class DistributionTicketSkuParamDTO {

    /**
     * 票档名称（例如 "内场 1280"、"看台 680"）
     */
    private String title;

    /**
     * 票档售价（单张票价）
     */
    private BigDecimal sellingPrice;

    /**
     * 票档总库存数量
     */
    private Integer totalStock;

    /**
     * 单人限购的门票张数上限
     */
    private Integer limitNum;
}
