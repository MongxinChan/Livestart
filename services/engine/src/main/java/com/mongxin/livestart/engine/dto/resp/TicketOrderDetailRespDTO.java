package com.mongxin.livestart.engine.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 订单详情响应 DTO
 */
@Data
@Schema(description = "订单详情响应")
public class TicketOrderDetailRespDTO {

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
     * 支付时间
     */
    @Schema(description = "支付时间")
    private Date payTime;

    /**
     * 下单时间
     */
    @Schema(description = "下单时间")
    private Date createTime;

    /**
     * 电子票列表
     */
    @Schema(description = "电子票列表")
    private List<TicketItemRespDTO> ticketItems;

    @Data
    @Schema(description = "电子票信息")
    public static class TicketItemRespDTO {
        /**
         * 电子票ID
         */
        @Schema(description = "电子票ID")
        private Long id;

        /**
         * 观演人ID
         */
        @Schema(description = "观演人ID")
        private Long visitorId;

        /**
         * 核销码
         */
        @Schema(description = "核销码")
        private String checkCode;

        /**
         * 是否已入场：0-未入场 1-已入场
         */
        @Schema(description = "是否已入场：0-未入场 1-已入场")
        private Integer isChecked;
    }
}
