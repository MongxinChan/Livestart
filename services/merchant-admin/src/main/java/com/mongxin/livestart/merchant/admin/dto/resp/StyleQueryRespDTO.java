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

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
