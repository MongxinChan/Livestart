package com.mongxin.livestart.engine.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 用户在前台提交的购票下单请求。
 * 包含目标票种、购买张数以及每张票绑定的观演人信息。
 */
@Data
@Schema(description = "购票下单请求")
public class TicketOrderCreateReqDTO {

    /**
     * 要购买的票种 ID。
     * 该值对应 merchant-admin 中维护的 t_ticket_sku 主键。
     */
    @NotNull(message = "票种ID不能为空")
    @Schema(description = "票种ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long skuId;

    /**
     * 本次下单的购票数量。
     * 当前前台下单链路限制每次最少 1 张，最多 6 张。
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量最少为1张")
    @Max(value = 6, message = "购买数量最多为6张")
    @Schema(description = "购买数量，取值范围 1~6", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer count;

    /**
     * 观演人 ID 列表。
     * 列表长度必须与 count 一致，每张票都要绑定一位实名观演人。
     */
    @NotNull(message = "观演人信息不能为空")
    @Schema(description = "观演人ID列表，数量必须与购买数量一致", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotNull(message = "观演人ID不能为空") Long> visitorIds;
}
