package com.mongxin.livestart.merchant.admin.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 演出创建接口请求参数
 */
@Data
@Schema(description = "演出创建参数")
public class EventSaveReqDTO {

    /**
     * 演出标题
     */
    @Schema(description = "演出标题", example = "2026新裤子乐队巡演·上海站", required = true)
    private String title;

    /**
     * 演出类型 0:Livehouse(站票) 1:演唱会(选座/ABCD区)
     */
    @Schema(description = "演出类型 0:Livehouse(站票) 1:演唱会(选座/ABCD区)", example = "0", required = true)
    private Integer eventType;

    /**
     * 关联场馆ID
     */
    @Schema(description = "关联场馆ID", example = "1", required = true)
    private Long venueId;

    /**
     * 演出开始时间
     */
    @Schema(description = "演出开始时间", example = "2026-08-15 20:00:00", required = true)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 海报图片地址
     */
    @Schema(description = "海报图片地址")
    private String posterUrl;

    @Schema(description = "关联演出歌手/艺人ID")
    private Long performerId;

    @Schema(description = "演出售票阶段 1:一开 2:二开", example = "1")
    private Integer ticketStage;
}

