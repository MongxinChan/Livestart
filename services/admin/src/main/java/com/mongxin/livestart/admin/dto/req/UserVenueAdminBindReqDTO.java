package com.mongxin.livestart.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserVenueAdminBindReqDTO {

    @NotNull(message = "\u7528\u6237ID\u4e0d\u80fd\u4e3a\u7a7a")
    private Long userId;

    @NotNull(message = "\u573a\u9986ID\u4e0d\u80fd\u4e3a\u7a7a")
    private Long venueId;
}
