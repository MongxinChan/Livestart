package com.mongxin.livestart.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 风格分页查询接口返回参数
 */
@Data
@Schema(description = "风格分页查询返回实体")
public class StylePageQueryRespDTO {

    @Schema(description = "风格ID")
    private Long id;

    @Schema(description = "风格名称")
    private String name;

    @Schema(description = "风格代码")
    private String code;

    @Schema(description = "风格描述")
    private String description;
}
