package com.mongxin.livestart.distribution.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 购票支付成功（出票通知）解耦本地副本事件 (用于分销微服务消费购票消息算分成)
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
     * 下单购买歌迷用户ID
     */
    private Long userId;

    /**
     * 支付网关交易流水号
     */
    private String tradeNo;
}
