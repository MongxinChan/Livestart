package com.mongxin.livestart.engine.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 演出列表响应（面向 C 端用户展示，含票档信息）
 */
@Data
@Schema(description = "演出列表项")
public class EventListRespDTO {

    @Schema(description = "演出 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "演出标题")
    private String title;

    @Schema(description = "演出类型：Livehouse / 演唱会")
    private String type;

    @Schema(description = "海报图片地址")
    private String cover;

    @Schema(description = "演出时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date date;

    @Schema(description = "场馆名称")
    private String venue;

    @Schema(description = "最低价")
    private BigDecimal minPrice;

    @Schema(description = "标签列表")
    private List<String> tags;

    @Schema(description = "票档规格列表")
    private List<TicketSkuRespDTO> skus;

    @Schema(description = "参演艺人名称")
    private String performerName;

    @Schema(description = "参演艺人/歌手")
    private String artist;

    @Schema(description = "演出售票阶段 1:一开 2:二开")
    private Integer ticketStage;

    @Schema(description = "演出城市")
    private String city;
}


