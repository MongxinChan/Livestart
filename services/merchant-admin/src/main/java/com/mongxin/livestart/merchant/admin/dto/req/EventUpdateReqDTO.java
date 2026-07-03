package com.mongxin.livestart.merchant.admin.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 演出修改请求参数传输对象 DTO。
 * 用于商家管理后台修改已有演出信息及重新配置售票阶段。
 */
@Data
@Schema(description = "演出修改参数")
public class EventUpdateReqDTO {

    /**
     * 演出 ID
     */
    @Schema(description = "演出 ID", required = true)
    private Long id;

    /**
     * 演出标题
     */
    @Schema(description = "演出标题")
    private String title;

    /**
     * 演出类型（0:Livehouse，1:演唱会）
     */
    @Schema(description = "演出类型 0:Livehouse 1:演唱会")
    private Integer eventType;

    /**
     * 关联场馆 ID
     */
    @Schema(description = "关联场馆 ID")
    private Long venueId;

    /**
     * 演出开始时间
     */
    @Schema(description = "演出开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    /**
     * 海报图片地址
     */
    @Schema(description = "海报图片地址")
    private String posterUrl;

    /**
     * 演出状态（0:下架，1:预售，2:在售，3:售罄）
     */
    @Schema(description = "演出状态 0:下架 1:预售 2:在售 3:售罄")
    private Integer status;

    /**
     * 关联演出歌手/艺人 ID
     */
    @Schema(description = "关联演出歌手/艺人 ID")
    private Long performerId;

    /**
     * 兼容字段：当前开票阶段（在多阶段售票下，以具体开售阶段配置为准。1:一开，2:二开）
     */
    @Schema(description = "兼容字段，当前开票阶段 1:一开 2:二开", example = "1")
    private Integer ticketStage;

    /**
     * 开售阶段列表（重新定义的各个售票时间段及序号）
     */
    @Schema(description = "开售阶段列表")
    private List<SaleStageReqDTO> saleStages;

    /**
     * 多选关联的风格 ID 集合（音乐风格/流派标签）
     */
    @Schema(description = "多选关联的风格 ID 集合")
    private List<Long> styleIds;
}
