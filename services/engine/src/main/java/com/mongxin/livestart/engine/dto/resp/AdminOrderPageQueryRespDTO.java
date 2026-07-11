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

    /**
     * 订单流水号
     */
    @Schema(description = "订单流水号")
    private String orderNo;

    /**
     * 下单用户ID
     */
    @Schema(description = "下单用户ID")
    private Long userId;

    /**
     * 下单用户名
     */
    @Schema(description = "下单用户名")
    private String username;

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
     * 场馆ID
     */
    @Schema(description = "场馆ID")
    private Long venueId;

    /**
     * 场馆名称
     */
    @Schema(description = "场馆名称")
    private String venueName;

    /**
     * 票种ID
     */
    @Schema(description = "票种ID")
    private Long skuId;

    /**
     * 票种名称
     */
    @Schema(description = "票种名称")
    private String skuTitle;

    /**
     * 购票数量
     */
    @Schema(description = "购票数量")
    private Integer ticketCount;

    /**
     * 实付总金额
     */
    @Schema(description = "实付总金额")
    private BigDecimal totalAmount;

    /**
     * 订单状态
     */
    @Schema(description = "订单状态")
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
}
