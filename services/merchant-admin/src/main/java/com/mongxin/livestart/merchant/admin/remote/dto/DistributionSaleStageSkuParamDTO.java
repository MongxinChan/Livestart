package com.mongxin.livestart.merchant.admin.remote.dto;

import lombok.Data;

/**
 * 商家管理后台向分销系统发布演出信息时，阶段票档配置的远程调用请求体 DTO。
 * 用于远程 Feign 接口间的数据传输。
 */
@Data
public class DistributionSaleStageSkuParamDTO {

    /**
     * 关联的票档名称（如 "看台 680"）
     */
    private String skuTitle;

    /**
     * 该阶段下该票档释放的库存数量
     */
    private Integer releaseStock;
}
