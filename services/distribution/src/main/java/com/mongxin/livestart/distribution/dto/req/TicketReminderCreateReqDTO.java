package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建开售提醒请求传输对象 DTO。
 * 用于接收客户端发起的用户预约特定售票阶段的开售提醒请求。
 */
@Data
@Schema(description = "创建开售提醒请求")
public class TicketReminderCreateReqDTO {

    /**
     * 关联的演出 ID
     */
    @NotNull(message = "eventId 不能为空")
    @Schema(description = "演出 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long eventId;

    /**
     * 关联的开售阶段 ID（对应指定的售票阶段，以便在对应的开售时间前发送短信）
     */
    @NotNull(message = "stageId 不能为空")
    @Schema(description = "开售阶段 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long stageId;
}
