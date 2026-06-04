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

    @Schema(description = "艺人头像")
    private String avatar;

    @Schema(description = "艺人介绍")
    private String bio;

    // ----- 前端兼容字段 -----
    @Schema(description = "头像图片 URL（前端列渲染映射）")
    private String avatarUrl;

    @Schema(description = "描述介绍（前端列渲染映射）")
    private String description;

    @Schema(description = "音乐风格流派名称（前端列渲染映射）")
    private String genre;

    @Schema(description = "多选关联的风格ID集合")
    private java.util.List<Long> styleIds;
}
