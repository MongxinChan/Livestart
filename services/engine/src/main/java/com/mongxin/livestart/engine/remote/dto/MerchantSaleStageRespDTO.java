package com.mongxin.livestart.engine.remote.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 引擎端引用的商户系统开售阶段响应 DTO
 */
@Data
public class MerchantSaleStageRespDTO {

    /**
     * 阶段序号（1:一开，2:二开...）
     */
    private Integer stageNo;

    /**
     * 阶段名称
     */
    private String stageName;

    /**
     * 该阶段统一开售时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    /**
     * 备注说明
     */
    private String remark;
}
