package com.mongxin.livestart.merchant.admin.remote.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 商家管理后台向分销系统发布演出信息时的远程调用请求体 DTO。
 * 用于远程 Feign 接口间的数据传输。
 */
@Data
public class DistributionEventPublishReqDTO {

    /**
     * 待发布的商户演出 ID，供分销服务记录演出来源。
     */
    private Long sourceEventId;

    /**
     * 演出标题
     */
    private String title;

    /**
     * 主演艺人 ID
     */
    private Long artistId;

    /**
     * 主演艺人姓名
     */
    private String artistName;

    /**
     * 演出开始时间
     */
    private Date eventTime;

    /**
     * 关联场馆 ID
     */
    private Long venueId;

    /**
     * 活动最早开售时间（多阶段售票下，可通过具体的开售阶段定义售票开始时间，本字段可选）
     */
    private Date saleStartTime;

    /**
     * 演出包含的所有票档参数配置列表
     */
    private List<DistributionTicketSkuParamDTO> skus;

    /**
     * 演出包含的所有开售阶段参数配置列表
     */
    private List<DistributionSaleStageParamDTO> saleStages;
}
