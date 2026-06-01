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

    @Schema(description = "订单流水号")
    private String orderNo;

    @Schema(description = "演出ID")
    private Long eventId;

    @Schema(description = "演出名称")
    private String eventTitle;

    @Schema(description = "票种名称")
    private String skuTitle;

    @Schema(description = "购买数量")
    private Integer count;

    @Schema(description = "实付总金额")
    private BigDecimal totalAmount;

    @Schema(description = "订单状态：0-待支付 1-已支付 2-已取消 3-已退票")
    private Integer status;

    @Schema(description = "订单状态描述")
    private String statusDesc;

    @Schema(description = "下单时间")
    private Date createTime;
}
