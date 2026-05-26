package com.mongxin.livestart.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 场馆新增/修改接口请求参数
 */
@Data
@Schema(description = "场馆新增/修改参数")
public class VenueSaveReqDTO {

    /**
     * 主键ID（修改时必传）
     */
    @Schema(description = "场馆ID（修改时必传）")
    private Long id;

    /**
     * 场馆名称
     */
    @Schema(description = "场馆名称", example = "万人体育馆", required = true)
    private String name;

    /**
     * 城市
     */
    @Schema(description = "城市", example = "上海", required = true)
    private String city;

    /**
     * 详细地址
     */
    @Schema(description = "详细地址", example = "上海市浦东新区XX路100号")
    private String address;

    /**
     * 场馆总容纳人数
     */
    @Schema(description = "场馆总容纳人数", example = "5000")
    private Integer capacity;
}
