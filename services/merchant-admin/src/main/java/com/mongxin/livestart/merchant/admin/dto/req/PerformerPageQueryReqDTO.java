package com.mongxin.livestart.merchant.admin.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 艺人分页查询接口请求参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "艺人分页查询参数")
public class PerformerPageQueryReqDTO extends Page {

    @Schema(description = "按名称模糊搜索")
    private String name;
}
