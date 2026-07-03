package com.mongxin.livestart.distribution.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 开售阶段参数 DTO。
 * 用于定义某一具体开售阶段（如一开、二开）的基础信息及对应的票档库存分配。
 */
@Data
@Schema(description = "开售阶段参数")
public class SaleStageParamDTO {

    /**
     * 阶段序号（1:一开，2:二开，3:三开...）
     */
    @Schema(description = "阶段序号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "阶段序号不能为空")
    private Integer stageNo;

    /**
     * 阶段名称（如 "首发开售"、"二次开售"）
     */
    @Schema(description = "阶段名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "预售第一阶段")
    @NotBlank(message = "阶段名称不能为空")
    private String stageName;

    /**
     * 阶段开售时间
     */
    @Schema(description = "阶段开售时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-06-15 10:00:00")
    @NotNull(message = "阶段开售时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    /**
     * 阶段备注
     */
    @Schema(description = "阶段备注")
    private String remark;

    /**
     * 该阶段各票档库存的释放配置
     */
    @Schema(description = "阶段票档释放配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "阶段票档释放配置不能为空")
    @Valid
    private List<SaleStageSkuParamDTO> skuConfigs;
}
