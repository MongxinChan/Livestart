package com.mongxin.livestart.engine.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 异步创建订单事件（消息载体）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketOrderCreateEvent {

    /** 订单流水号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 票档SKU ID */
    private Long skuId;

    /** 购票张数 */
    private Integer count;

    /** 观演人ID列表 */
    private List<Long> visitorIds;
}
