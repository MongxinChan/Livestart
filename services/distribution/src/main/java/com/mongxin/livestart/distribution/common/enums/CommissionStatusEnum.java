package com.mongxin.livestart.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 艺人分销票房提成结算状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum CommissionStatusEnum {

    /**
     * 待结算 (歌迷下单支付成功)
     */
    PENDING(0, "待结算"),

    /**
     * 已结算到账 (演出核销完毕或无退款)
     */
    SETTLED(1, "已结算"),

    /**
     * 已取消 (歌迷退票分成作废)
     */
    CANCELLED(2, "已取消");

    private final int code;
    private final String desc;
}
