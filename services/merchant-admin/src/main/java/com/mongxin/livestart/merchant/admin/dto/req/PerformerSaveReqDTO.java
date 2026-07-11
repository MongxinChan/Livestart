package com.mongxin.livestart.merchant.admin.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 艺人/乐队新增/修改接口请求参数
 */
@Data
@Schema(description = "艺人/乐队新增/修改参数")
public class PerformerSaveReqDTO {

    /**
     * 艺人ID（修改时必传）
     */
    @Schema(description = "艺人ID（修改时必传）")
    private Long id;

    /**
     * 艺人/乐队名称
     */
    @Schema(description = "艺人/乐队名称", example = "新裤子乐队", required = true)
    private String name;

    /**
     * 关联风格ID
     */
    @Schema(description = "关联风格ID", example = "1")
    private Long styleId;

    /**
     * 艺人头像/Logo URL
     */
    @Schema(description = "艺人头像/Logo URL")
    private String avatar;

    /**
     * 介绍
     */
    @Schema(description = "介绍")
    private String bio;

    /**
     * 状态 1:正常 0:停演
     */
    @Schema(description = "状态 1:正常 0:停演", example = "1")
    private Integer status;

    // ----- 前端兼容与自适应字段 -----
    /**
     * 头像图片 URL（前端字段名映射）
     */
    @Schema(description = "头像图片 URL（前端字段名映射）")
    private String avatarUrl;

    /**
     * 描述介绍（前端字段名映射）
     */
    @Schema(description = "描述介绍（前端字段名映射）")
    private String description;

    /**
     * 音乐风格流派名称（前端字段名映射，自动隐式查表创建）
     */
    @Schema(description = "音乐风格流派名称（前端字段名映射，自动隐式查表创建）")
    private String genre;

    /**
     * 多选关联的风格ID集合
     */
    @Schema(description = "多选关联的风格ID集合")
    private java.util.List<Long> styleIds;
}
