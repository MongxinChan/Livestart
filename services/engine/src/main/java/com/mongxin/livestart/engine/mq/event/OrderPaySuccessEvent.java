package com.mongxin.livestart.engine.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付成功（出票）异步事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderPaySuccessEvent {

    /**
     * 订单流水号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 第三方支付流水号
     */
    private String tradeNo;
}
