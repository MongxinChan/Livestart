package com.mongxin.livestart.merchant.admin.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 风格分页查询接口返回参数
 */
@Data
@Schema(description = "风格分页查询返回实体")
public class StylePageQueryRespDTO {

    /**
     * 风格ID
     */
    @Schema(description = "风格ID")
    private Long id;

    /**
     * 风格名称
     */
    @Schema(description = "风格名称")
    private String name;

    /**
     * 风格代码
     */
    @Schema(description = "风格代码")
    private String code;

    /**
     * 风格描述
     */
    @Schema(description = "风格描述")
    private String description;
}
