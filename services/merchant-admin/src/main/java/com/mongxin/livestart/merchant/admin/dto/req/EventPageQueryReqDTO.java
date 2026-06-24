package com.mongxin.livestart.merchant.admin.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 演出分页查询接口请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "演出分页查询参数")
public class EventPageQueryReqDTO extends Page {

    /**
     * 按演出状态筛选
     */
    @Schema(description = "按演出状态筛选 0:下架 1:预售 2:在售 3:售罄")
    private Integer status;
}
