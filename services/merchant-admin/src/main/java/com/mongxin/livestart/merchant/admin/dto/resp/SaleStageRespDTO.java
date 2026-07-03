package com.mongxin.livestart.merchant.admin.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 商家后台开售阶段返回参数传输对象 DTO。
 * 用于展示演出包含的各个开售阶段的配置详情。
 */
@Data
@Schema(description = "商家后台开售阶段返回参数")
public class SaleStageRespDTO {

    /**
     * 阶段序号（1:一开，2:二开，3:三开...）
     */
    @Schema(description = "阶段序号")
    private Integer stageNo;

    /**
     * 阶段名称（如 "首发开售"、"二次开售"）
     */
    @Schema(description = "阶段名称")
    private String stageName;

    /**
     * 该阶段的开售时间
     */
    @Schema(description = "阶段开售时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    /**
     * 备注说明
     */
    @Schema(description = "备注")
    private String remark;
}
