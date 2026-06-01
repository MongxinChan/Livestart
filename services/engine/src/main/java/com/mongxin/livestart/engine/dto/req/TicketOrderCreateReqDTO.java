package com.mongxin.livestart.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 购票下单请求 DTO
 */
@Data
@Schema(description = "购票下单请求")
public class TicketOrderCreateReqDTO {

    /**
     * 票种ID
     */
    @NotNull(message = "票种ID不能为空")
    @Schema(description = "票种ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long skuId;

    /**
     * 购买数量（1~6）
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量最少为1张")
    @Max(value = 6, message = "购买数量最多为6张")
    @Schema(description = "购买数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer count;

    /**
     * 观演人ID列表（每张票对应一位观演人）
     */
    @NotNull(message = "观演人信息不能为空")
    @Schema(description = "观演人ID列表，数量须与购买数量一致", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotNull Long> visitorIds;
}
