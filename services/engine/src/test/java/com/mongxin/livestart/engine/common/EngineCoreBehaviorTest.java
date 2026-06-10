package com.mongxin.livestart.engine.common;

import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.common.enums.StockDecrementErrorEnum;
import com.mongxin.livestart.engine.toolkit.StockDecrementReturnCombinedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EngineCoreBehaviorTest {

    private static long combine(long errorCode, long currentCount) {
        return (errorCode << 14) + currentCount;
    }

    @Test
    void shouldDecodeLuaResultForLimitExceeded() {
        long combined = combine(2L, 3L);

        assertEquals(2L, StockDecrementReturnCombinedUtil.extractErrorCode(combined));
        assertEquals(3L, StockDecrementReturnCombinedUtil.extractCurrentCount(combined));
        assertEquals(StockDecrementErrorEnum.LIMIT_EXCEEDED, StockDecrementErrorEnum.fromCode(2L));
        assertTrue(StockDecrementErrorEnum.isFail(2L));
    }

    @Test
    void shouldTreatNullLuaResultAsStockFailure() {
        assertEquals(1L, StockDecrementReturnCombinedUtil.extractErrorCode(null));
        assertEquals(0L, StockDecrementReturnCombinedUtil.extractCurrentCount(null));
        assertEquals(StockDecrementErrorEnum.STOCK_INSUFFICIENT, StockDecrementErrorEnum.fromCode(999L));
    }

    @Test
    void shouldResolveOrderStatusByCode() {
        assertEquals(OrderStatusEnum.PAID, OrderStatusEnum.fromCode(1));
        assertThrows(IllegalArgumentException.class, () -> OrderStatusEnum.fromCode(99));
    }
}
