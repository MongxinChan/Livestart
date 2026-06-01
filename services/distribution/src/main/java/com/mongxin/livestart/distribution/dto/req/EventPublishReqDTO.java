package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 演唱会发布请求 DTO
 */
@Data
@Schema(description = "演唱会演出发布请求")
public class EventPublishReqDTO {

    @Schema(description = "演出标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "周杰伦 2026 嘉年华演唱会")
    @NotBlank(message = "演出标题不能为空")
    private String title;

    @Schema(description = "主演艺人ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9527")
    @NotNull(message = "主演艺人ID不能为空")
    private Long artistId;

    @Schema(description = "主演艺人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "周杰伦")
    @NotBlank(message = "艺人姓名不能为空")
    private String artistName;

    @Schema(description = "演出开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "演出开始时间不能为空")
    private Date eventTime;

    @Schema(description = "演出地点/场馆", requiredMode = Schema.RequiredMode.REQUIRED, example = "深圳大运中心体育场")
    @NotBlank(message = "演出地点不能为空")
    private String address;

    @Schema(description = "门票开售时间（不传则立即开售）", example = "2026-06-15 10:00:00")
    private Date saleStartTime;

    @Schema(description = "票档库存设置", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "门票票档不能为空")
    @Valid
    private List<TicketSkuParam> skus;

    @Data
    @Schema(description = "门票票档参数")
    public static class TicketSkuParam {

        @Schema(description = "票档名称 (如: 看台680, 内场1280)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "票档名称不能为空")
        private String title;

        @Schema(description = "门票售价", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "售价不能为空")
        private BigDecimal sellingPrice;

        @Schema(description = "发售总库存", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "发售库存不能为空")
        private Integer totalStock;

        @Schema(description = "单人限购张数", defaultValue = "2")
        private Integer limitNum;
    }
}
