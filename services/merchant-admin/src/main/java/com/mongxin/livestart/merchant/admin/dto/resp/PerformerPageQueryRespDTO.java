package com.mongxin.livestart.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 艺人分页查询接口返回参数
 */
@Data
@Schema(description = "艺人分页查询返回实体")
public class PerformerPageQueryRespDTO {

    @Schema(description = "艺人ID")
    private Long id;

    @Schema(description = "艺人/乐队名称")
    private String name;

    @Schema(description = "关联风格ID")
    private Long styleId;

    @Schema(description = "状态 1:正常 0:停演")
    private Integer status;
}
