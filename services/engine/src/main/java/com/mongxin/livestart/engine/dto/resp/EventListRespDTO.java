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
 * Event list response for the client application.
 */
@Data
@Schema(description = "Event list item")
public class EventListRespDTO {

    /**
     * 演出活动ID
     */
    @Schema(description = "Event id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 演出标题
     */
    @Schema(description = "Event title")
    private String title;

    /**
     * 音乐风格/演出类型
     */
    @Schema(description = "Event type")
    private String type;

    /**
     * 演出海报/封面图片URL
     */
    @Schema(description = "Poster url")
    private String cover;

    /**
     * 演出开始时间
     */
    @Schema(description = "Event start time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date date;

    /**
     * 当前售票阶段状态文本描述
     */
    @Schema(description = "Current sale-stage status text")
    private String statusText;

    /**
     * 演出是否已开始
     */
    @Schema(description = "Whether the event has already started")
    private Boolean started;

    /**
     * 场馆名称
     */
    @Schema(description = "Venue name")
    private String venue;

    /**
     * 最低起售价
     */
    @Schema(description = "Lowest price")
    private BigDecimal minPrice;

    /**
     * 演出标签列表
     */
    @Schema(description = "Tags")
    private List<String> tags;

    /**
     * 演出票档规格列表
     */
    @Schema(description = "Ticket sku list")
    private List<TicketSkuRespDTO> skus;

    /**
     * 艺人/团体名称
     */
    @Schema(description = "Performer name")
    private String performerName;

    /**
     * 主演艺人名称
     */
    @Schema(description = "Artist name")
    private String artist;

    /**
     * 票务售票阶段 (1: 一阶段售票, 2: 二阶段售票)
     */
    @Schema(description = "Ticket stage 1:first sale 2:second sale")
    private Integer ticketStage;

    /**
     * 所在城市
     */
    @Schema(description = "City")
    private String city;

    /**
     * 演出状态：0: 下架, 1: 预售中, 2: 售票中, 3: 已售罄
     */
    @Schema(description = "Event status 0:off shelf 1:presale 2:on sale 3:sold out")
    private Integer status;
}
