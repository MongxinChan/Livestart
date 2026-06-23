package com.mongxin.livestart.engine.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 后台订单分页列表项响应
 */
@Data
@Schema(description = "后台订单分页列表项响应")
public class AdminOrderPageQueryRespDTO {

    @Schema(description = "订单流水号")
    private String orderNo;

    @Schema(description = "下单用户ID")
    private Long userId;

    @Schema(description = "下单用户名")
    private String username;

    @Schema(description = "演出ID")
    private Long eventId;

    @Schema(description = "演出名称")
    private String eventTitle;

    @Schema(description = "票种ID")
    private Long skuId;

    @Schema(description = "票种名称")
    private String skuName;

    @Schema(description = "购票数量")
    private Integer ticketCount;

    @Schema(description = "实付总金额")
    private BigDecimal totalAmount;

    @Schema(description = "订单状态")
    private Integer status;

    @Schema(description = "订单状态描述")
    private String statusDesc;

    @Schema(description = "下单时间")
    private Date createTime;
}
