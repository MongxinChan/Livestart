package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 演出票档参数 DTO。
 * 用于定义某一演出下各票档的基础配置（如标题、售价、总库存、限购限制）。
 */
@Data
@Schema(description = "票档参数")
public class TicketSkuParam {

    /**
     * 票档名称（例如 "内场 1280"、"看台 680"）
     */
    @Schema(description = "票档名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "看台 680")
    @NotBlank(message = "票档名称不能为空")
    private String title;

    /**
     * 票档售价（单张票价）
     */
    @Schema(description = "票档售价", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "售价不能为空")
    private BigDecimal sellingPrice;

    /**
     * 该票档的总库存数量
     */
    @Schema(description = "票档总库存", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "票档总库存不能为空")
    private Integer totalStock;

    /**
     * 单个用户限购的门票张数上限（默认为 2）
     */
    @Schema(description = "单人限购张数", defaultValue = "2")
    private Integer limitNum;
}
