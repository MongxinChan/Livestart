package com.mongxin.livestart.distribution.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 歌迷持有门票使用状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum TicketStatusEnum {

    /**
     * 未使用/有效可核销
     */
    UNUSED(0, "未使用"),

    /**
     * 已核销使用
     */
    USED(1, "已使用"),

    /**
     * 已退票作废
     */
    REFUNDED(2, "已退票");

    private final int code;
    private final String desc;
}
