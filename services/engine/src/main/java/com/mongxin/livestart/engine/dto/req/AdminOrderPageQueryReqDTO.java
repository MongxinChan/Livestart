package com.mongxin.livestart.engine.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台订单分页查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台订单分页查询请求")
public class AdminOrderPageQueryReqDTO extends Page<Object> {

    @Schema(description = "订单状态筛选：1-待支付 2-已出票 3-已取消 4-已退票，不传表示全部")
    private Integer status;

    @Schema(description = "演出 ID 筛选")
    private Long eventId;

    @Schema(description = "场馆 ID 筛选")
    private Long venueId;
}
