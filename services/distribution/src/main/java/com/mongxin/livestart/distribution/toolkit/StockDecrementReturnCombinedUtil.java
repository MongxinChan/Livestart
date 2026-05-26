package com.mongxin.livestart.distribution.toolkit;

/**
 * Lua 脚本返回值位移二进制解包工具类 (门票预扣库存版)
 * <p>
 * 将错误码和当前领购数量打包为一个 Long，并在 Java 端以位移迅速拆解
 * - 高位 (右移 14 位)：错误码 (0=成功, 1=库存不足, 2=超出个人限额)
 * - 低位 (低 14 位)：用户当前的已领已购数
 */
public final class StockDecrementReturnCombinedUtil {

    private static final int SECOND_FIELD_BITS = 14;
    private static final long SECOND_FIELD_MASK = (1L << SECOND_FIELD_BITS) - 1;

    private StockDecrementReturnCombinedUtil() {}

    /**
     * 提取高位 (错误码)
     */
    public static long extractErrorCode(Long combined) {
        if (combined == null) {
            return 1L;
        }
        return combined >> SECOND_FIELD_BITS;
    }

    /**
     * 提取低位 (已抢已领数)
     */
    public static long extractCurrentCount(Long combined) {
        if (combined == null) {
            return 0L;
        }
        return combined & SECOND_FIELD_MASK;
    }
}
