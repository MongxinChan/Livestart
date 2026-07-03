package com.mongxin.livestart.merchant.admin.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 商家后台开售阶段请求参数传输对象 DTO。
 * 用于在创建或修改演出时，接收前端传入的各个开售阶段配置。
 */
@Data
@Schema(description = "商家后台开售阶段参数")
public class SaleStageReqDTO {

    /**
     * 阶段序号（1:一开，2:二开，3:三开...）
     */
    @Schema(description = "阶段序号", example = "1")
    private Integer stageNo;

    /**
     * 阶段名称（如 "首发开售"、"二次开售"）
     */
    @Schema(description = "阶段名称", example = "预售第一阶段")
    private String stageName;

    /**
     * 阶段统一开售时间
     */
    @Schema(description = "阶段开售时间", example = "2026-08-15 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    /**
     * 该阶段的备注说明
     */
    @Schema(description = "备注")
    private String remark;
}
