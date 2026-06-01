package com.mongxin.livestart.engine.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单超时关单延时事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDelayCloseEvent {

    /**
     * 订单流水号
     */
    private String orderNo;

    /**
     * 用户ID（分片键，关单时路由到正确分片）
     */
    private Long userId;

    /**
     * 票种ID（归还库存使用）
     */
    private Long skuId;

    /**
     * 购买数量（归还库存使用）
     */
    private Integer count;

    /**
     * 延时投递时间戳（ms），即订单到期时间
     */
    private Long delayTime;
}
