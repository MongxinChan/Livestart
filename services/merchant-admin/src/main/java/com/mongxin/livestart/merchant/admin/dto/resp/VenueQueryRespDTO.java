package com.mongxin.livestart.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 场馆详情查询接口返回参数
 */
@Data
@Schema(description = "场馆详情查询返回实体")
public class VenueQueryRespDTO {

    /**
     * 场馆ID
     */
    @Schema(description = "场馆ID")
    private Long id;

    /**
     * 场馆名称
     */
    @Schema(description = "场馆名称")
    private String name;

    /**
     * 城市
     */
    @Schema(description = "城市")
    private String city;

    /**
     * 详细地址
     */
    @Schema(description = "详细地址")
    private String address;

    /**
     * 场馆总容纳人数
     */
    @Schema(description = "场馆总容纳人数")
    private Integer capacity;

    /**
     * 归属场地管理员用户 ID
     */
    @Schema(description = "归属场地管理员用户 ID")
    private Long ownerUserId;
}
