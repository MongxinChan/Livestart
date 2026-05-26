package com.mongxin.livestart.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 艺人/乐队新增/修改接口请求参数
 */
@Data
@Schema(description = "艺人/乐队新增/修改参数")
public class PerformerSaveReqDTO {

    @Schema(description = "艺人ID（修改时必传）")
    private Long id;

    @Schema(description = "艺人/乐队名称", example = "新裤子乐队", required = true)
    private String name;

    @Schema(description = "关联风格ID", example = "1")
    private Long styleId;

    @Schema(description = "艺人头像/Logo URL")
    private String avatar;

    @Schema(description = "介绍")
    private String bio;

    @Schema(description = "状态 1:正常 0:停演", example = "1")
    private Integer status;
}
