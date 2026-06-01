package com.mongxin.livestart.engine.toolkit;

/**
 * Lua 脚本返回值解析工具
 * <p>
 * Lua 将两个字段编码为单个 Long：
 * - 高位（右移 14 位）：错误码（0=成功，1=库存不足，2=超限额）
 * - 低位（低 14 位）：当前用户已购数量
 */
public final class StockDecrementReturnCombinedUtil {

    private static final int SECOND_FIELD_BITS = 14;
    private static final long SECOND_FIELD_MASK = (1L << SECOND_FIELD_BITS) - 1;

    /**
     * 提取高位（错误码）
     */
    public static long extractErrorCode(Long combined) {
        if (combined == null) {
            return 1L;
        }
        return combined >> SECOND_FIELD_BITS;
    }

    /**
     * 提取低位（当前用户已购数量）
     */
    public static long extractCurrentCount(Long combined) {
        if (combined == null) {
            return 0L;
        }
        return combined & SECOND_FIELD_MASK;
    }

    private StockDecrementReturnCombinedUtil() {
    }
}
