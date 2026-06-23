package com.mongxin.livestart.distribution.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Create reminder request.
 */
@Data
@Schema(description = "Create ticket reminder request")
public class TicketReminderCreateReqDTO {

    @NotNull(message = "eventId must not be null")
    @Schema(description = "Event id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long eventId;
}
