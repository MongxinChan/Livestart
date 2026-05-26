package com.mongxin.livestart.merchant.admin.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 场馆分页查询接口请求参数
 */
@Data
@Schema(description = "场馆分页查询参数")
public class VenuePageQueryReqDTO extends Page {

    /**
     * 按城市筛选
     */
    @Schema(description = "按城市筛选")
    private String city;
}
