package com.mongxin.livestart.admin.dto.req;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 场地管理员绑定请求参数
 */
@Data
public class UserVenueAdminBindReqDTO {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 场馆ID
     */
    @NotNull(message = "场馆ID不能为空")
    private Long venueId;
}
