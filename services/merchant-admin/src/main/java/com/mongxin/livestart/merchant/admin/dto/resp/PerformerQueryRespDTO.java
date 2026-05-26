package com.mongxin.livestart.merchant.admin.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 艺人详情查询接口返回参数
 */
@Data
@Schema(description = "艺人详情查询返回实体")
public class PerformerQueryRespDTO {

    @Schema(description = "艺人ID")
    private Long id;

    @Schema(description = "艺人/乐队名称")
    private String name;

    @Schema(description = "关联风格ID")
    private Long styleId;

    @Schema(description = "艺人头像/Logo URL")
    private String avatar;

    @Schema(description = "介绍")
    private String bio;

    @Schema(description = "状态 1:正常 0:停演")
    private Integer status;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
