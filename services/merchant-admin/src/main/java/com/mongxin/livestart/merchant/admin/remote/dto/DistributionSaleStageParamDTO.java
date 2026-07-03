package com.mongxin.livestart.merchant.admin.remote.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 商家管理后台向分销系统发布演出信息时，开售阶段的远程调用请求体 DTO。
 * 用于远程 Feign 接口间的数据传输。
 */
@Data
public class DistributionSaleStageParamDTO {

    /**
     * 阶段序号（1:一开，2:二开，3:三开...）
     */
    private Integer stageNo;

    /**
     * 阶段名称（如 "首发开售"、"二次开售"）
     */
    private String stageName;

    /**
     * 阶段开售时间
     */
    private Date saleStartTime;

    /**
     * 该阶段的备注说明
     */
    private String remark;

    /**
     * 该阶段下各个票档库存释放的配置参数
     */
    private List<DistributionSaleStageSkuParamDTO> skuConfigs;
}
