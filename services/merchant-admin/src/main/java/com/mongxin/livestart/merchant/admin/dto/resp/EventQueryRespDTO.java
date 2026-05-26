package com.mongxin.livestart.merchant.admin.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 演出详情查询接口返回参数
 */
@Data
@Schema(description = "演出详情查询返回实体")
public class EventQueryRespDTO {

    @Schema(description = "演出ID")
    private Long id;

    @Schema(description = "演出标题")
    private String title;

    @Schema(description = "演出类型 0:Livehouse(站票) 1:演唱会(选座/ABCD区)")
    private Integer eventType;

    @Schema(description = "关联场馆ID")
    private Long venueId;

    @Schema(description = "演出开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @Schema(description = "海报图片地址")
    private String posterUrl;

    @Schema(description = "状态 0:下架 1:预售 2:在售 3:售罄")
    private Integer status;
}
