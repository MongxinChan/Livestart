package com.mongxin.livestart.engine.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Redis 库存扣减错误枚举
 * <p>
 * 与 Lua 脚本返回值对应：
 * 0 = 成功
 * 1 = 库存不足
 * 2 = 超出个人购票限额
 */
@Getter
@RequiredArgsConstructor
public enum StockDecrementErrorEnum {

    SUCCESS(0, "扣减成功"),
    STOCK_INSUFFICIENT(1, "票种库存不足，请选择其他票种"),
    LIMIT_EXCEEDED(2, "超出单人最大购票数量限制");

    private final int code;
    private final String message;

    public static boolean isFail(long code) {
        return code != SUCCESS.code;
    }

    public static StockDecrementErrorEnum fromCode(long code) {
        for (StockDecrementErrorEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return STOCK_INSUFFICIENT;
    }
}
