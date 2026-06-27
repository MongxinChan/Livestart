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
 * 演出发布请求传输对象 DTO。
 * 用于从商家后台分发/发布演出信息至分销系统。
 */
@Data
@Schema(description = "演出发布请求")
public class EventPublishReqDTO {

    /**
     * 演出标题
     */
    @Schema(description = "演出标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "周杰伦 2026 嘉年华演唱会")
    @NotBlank(message = "演出标题不能为空")
    private String title;

    /**
     * 主演艺人 ID
     */
    @Schema(description = "主演艺人 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "9527")
    @NotNull(message = "主演艺人 ID 不能为空")
    private Long artistId;

    /**
     * 主演艺人姓名
     */
    @Schema(description = "主演艺人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "周杰伦")
    @NotBlank(message = "艺人姓名不能为空")
    private String artistName;

    /**
     * 演出开始时间
     */
    @Schema(description = "演出开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "演出开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date eventTime;

    /**
     * 关联场馆 ID
     */
    @Schema(description = "关联场馆 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "101001")
    @NotNull(message = "关联场馆 ID 不能为空")
    private Long venueId;

    /**
     * 兼容字段：活动最早开售时间（多阶段售票下以第一阶段开售时间为准，本字段可选传）
     */
    @Schema(description = "兼容字段，活动最早开售时间可不传", example = "2026-06-15 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date saleStartTime;

    /**
     * 票档配置列表
     */
    @Schema(description = "票档配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "票档不能为空")
    @Valid
    private List<TicketSkuParam> skus;

    /**
     * 开售阶段配置列表（支持多阶段，如：一开、二开）
     */
    @Schema(description = "开售阶段配置", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "开售阶段不能为空")
    @Valid
    private List<SaleStageParamDTO> saleStages;
}
