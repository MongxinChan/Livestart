package com.mongxin.livestart.distribution.common;

import com.mongxin.livestart.distribution.common.enums.TicketStatusEnum;
import com.mongxin.livestart.distribution.toolkit.StockDecrementReturnCombinedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistributionCoreBehaviorTest {

    private static long combine(long errorCode, long currentCount) {
        return (errorCode << 14) + currentCount;
    }

    @Test
    void shouldDecodeLuaResultForDistributionLimit() {
        long combined = combine(2L, 1L);

        assertEquals(2L, StockDecrementReturnCombinedUtil.extractErrorCode(combined));
        assertEquals(1L, StockDecrementReturnCombinedUtil.extractCurrentCount(combined));
    }

    @Test
    void shouldKeepTicketStatusCodeContractStable() {
        assertEquals(0, TicketStatusEnum.UNUSED.getCode());
        assertEquals(1, TicketStatusEnum.USED.getCode());
        assertEquals(2, TicketStatusEnum.REFUNDED.getCode());
    }
}
