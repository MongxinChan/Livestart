package com.mongxin.livestart.merchant.admin.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 风格详情查询接口返回参数
 */
@Data
@Schema(description = "风格详情查询返回实体")
public class StyleQueryRespDTO {

    @Schema(description = "风格ID")
    private Long id;

    @Schema(description = "风格名称")
    private String name;

    @Schema(description = "风格代码")
    private String code;

    @Schema(description = "风格描述")
    private String description;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
