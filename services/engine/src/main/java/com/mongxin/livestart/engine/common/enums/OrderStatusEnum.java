package com.mongxin.livestart.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatusEnum {

    PENDING_PAYMENT(0, "待支付"),
    PAID(1, "已支付"),
    CANCELLED(2, "已取消"),
    REFUNDED(3, "已退票");

    private final int code;
    private final String desc;

    public static OrderStatusEnum fromCode(int code) {
        for (OrderStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("未知订单状态码: " + code);
    }
}
