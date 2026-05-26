package com.mongxin.livestart.distribution.mq.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 艺人宣发提成及个税代扣异步结算/入账事件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommissionSettleEvent {

    /**
     * 票房提成明细记录ID
     */
    private Long commissionRecordId;

    /**
     * 关联歌迷购买订单流水号
     */
    private String orderNo;

    /**
     * 动作 1:代扣个税完成票房分成正式入账到可用余额 2:歌迷退票分成明细取消
     */
    private Integer action;
}
