package com.mongxin.livestart.engine.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 我的订单分页列表项响应 DTO
 */
@Data
@Schema(description = "我的订单列表项响应")
public class TicketOrderPageQueryRespDTO {

    /**
     * 订单流水号
     */
    @Schema(description = "订单流水号")
    private String orderNo;

    /**
     * 演出ID
     */
    @Schema(description = "演出ID")
    private Long eventId;

    /**
     * 演出名称
     */
    @Schema(description = "演出名称")
    private String eventTitle;

    /**
     * 票种名称
     */
    @Schema(description = "票种名称")
    private String skuTitle;

    /**
     * 票档单价
     */
    @Schema(description = "票档单价")
    private BigDecimal price;

    /**
     * 购买数量
     */
    @Schema(description = "购买数量")
    private Integer count;

    /**
     * 实付总金额
     */
    @Schema(description = "实付总金额")
    private BigDecimal totalAmount;

    /**
     * 订单状态：0-待支付 1-已支付 2-已取消 3-已退票
     */
    @Schema(description = "订单状态：0-待支付 1-已支付 2-已取消 3-已退票")
    private Integer status;

    /**
     * 订单状态描述
     */
    @Schema(description = "订单状态描述")
    private String statusDesc;

    /**
     * 下单时间
     */
    @Schema(description = "下单时间")
    private Date createTime;
    /**
     * 首张电子票核销码，用于用户出示和现场验票。
     */
    @Schema(description = "首张电子票核销码")
    private String checkCode;

    /**
     * 首张电子票入场状态：0-未入场 1-已入场。
     */
    @Schema(description = "首张电子票入场状态：0-未入场 1-已入场")
    private Integer isChecked;
}
