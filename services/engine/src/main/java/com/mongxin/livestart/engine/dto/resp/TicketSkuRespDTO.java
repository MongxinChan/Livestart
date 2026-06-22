package com.mongxin.livestart.engine.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 票档信息响应（面向 C 端用户展示）
 */
@Data
@Schema(description = "票档信息")
public class TicketSkuRespDTO {

    @Schema(description = "票档 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @Schema(description = "票档名称")
    private String name;

    @Schema(description = "售价")
    private BigDecimal price;

    @Schema(description = "剩余库存")
    private Integer stock;

    @Schema(description = "总库存")
    private Integer total;
}
