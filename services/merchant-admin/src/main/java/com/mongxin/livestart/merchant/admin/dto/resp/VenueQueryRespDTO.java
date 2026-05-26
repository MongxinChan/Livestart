package com.mongxin.livestart.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 场馆详情查询接口返回参数
 */
@Data
@Schema(description = "场馆详情查询返回实体")
public class VenueQueryRespDTO {

    @Schema(description = "场馆ID")
    private Long id;

    @Schema(description = "场馆名称")
    private String name;

    @Schema(description = "城市")
    private String city;

    @Schema(description = "详细地址")
    private String address;

    @Schema(description = "场馆总容纳人数")
    private Integer capacity;
}
