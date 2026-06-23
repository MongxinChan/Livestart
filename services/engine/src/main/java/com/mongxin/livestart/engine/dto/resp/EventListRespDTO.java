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

    @Schema(description = "Event id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "Event title")
    private String title;

    @Schema(description = "Event type")
    private String type;

    @Schema(description = "Poster url")
    private String cover;

    @Schema(description = "Event start time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date date;

    @Schema(description = "Current sale-stage status text")
    private String statusText;

    @Schema(description = "Whether the event has already started")
    private Boolean started;

    @Schema(description = "Venue name")
    private String venue;

    @Schema(description = "Lowest price")
    private BigDecimal minPrice;

    @Schema(description = "Tags")
    private List<String> tags;

    @Schema(description = "Ticket sku list")
    private List<TicketSkuRespDTO> skus;

    @Schema(description = "Performer name")
    private String performerName;

    @Schema(description = "Artist name")
    private String artist;

    @Schema(description = "Ticket stage 1:first sale 2:second sale")
    private Integer ticketStage;

    @Schema(description = "City")
    private String city;

    @Schema(description = "Event status 0:off shelf 1:presale 2:on sale 3:sold out")
    private Integer status;
}
