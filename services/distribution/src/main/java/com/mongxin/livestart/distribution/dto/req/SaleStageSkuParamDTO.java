package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 阶段票档释放参数 DTO。
 * 用于定义在特定开售阶段下，具体某个票档（SKU）所释放的库存。
 */
@Data
@Schema(description = "阶段票档释放参数")
public class SaleStageSkuParamDTO {

    /**
     * 票档名称（例如 "看台 680"）
     */
    @Schema(description = "票档名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "看台 680")
    @NotBlank(message = "票档名称不能为空")
    private String skuTitle;

    /**
     * 该阶段释放库存数量
     */
    @Schema(description = "释放库存", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "释放库存不能为空")
    private Integer releaseStock;
}
