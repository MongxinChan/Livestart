package com.mongxin.livestart.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 风格新增/修改接口请求参数
 */
@Data
@Schema(description = "风格新增/修改参数")
public class StyleSaveReqDTO {

    @Schema(description = "风格ID（修改时必传）")
    private Long id;

    @Schema(description = "风格名称", example = "摇滚", required = true)
    private String name;

    @Schema(description = "风格代码", example = "ROCK", required = true)
    private String code;

    @Schema(description = "风格描述", example = "以强烈的节奏和电吉他为主的音乐流派")
    private String description;
}
