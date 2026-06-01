package com.mongxin.livestart.engine.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的订单分页查询请求 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "我的订单分页查询请求")
public class TicketOrderPageQueryReqDTO extends Page<Object> {

    /**
     * 订单状态筛选（null 表示全部）
     */
    @Schema(description = "订单状态筛选：0-待支付 1-已支付 2-已取消 3-已退票，不传表示全部")
    private Integer status;
}
