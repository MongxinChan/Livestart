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

    /**
     * 票档 ID
     */
    @Schema(description = "票档 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 票档名称
     */
    @Schema(description = "票档名称")
    private String name;

    /**
     * 售价
     */
    @Schema(description = "售价")
    private BigDecimal price;

    /**
     * 剩余库存
     */
    @Schema(description = "剩余库存")
    private Integer stock;

    /**
     * 总库存
     */
    @Schema(description = "总库存")
    private Integer total;

    /**
     * 一开释放库存
     */
    @Schema(description = "一开释放库存")
    private Integer stage1Stock;

    /**
     * 二开释放库存
     */
    @Schema(description = "二开释放库存")
    private Integer stage2Stock;
}
