package com.mongxin.livestart.engine.dao.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTableHashModShardingAlgorithmTest {

    private final OrderTableHashModShardingAlgorithm algorithm = new OrderTableHashModShardingAlgorithm();
    private final List<String> availableTables = List.of(
            "t_order_0", "t_order_1", "t_order_2", "t_order_3",
            "t_order_4", "t_order_5", "t_order_6", "t_order_7",
            "t_order_8", "t_order_9", "t_order_10", "t_order_11",
            "t_order_12", "t_order_13", "t_order_14", "t_order_15"
    );

    @Test
    void shouldRouteToExpectedShardForRealUserIds() {
        assertRoute(2069617292963614721L);
        assertRoute(2070046199038844929L);
        assertRoute(2070044233629888513L);
    }

    @Test
    void shouldUsePositiveLongHashModuloForTableIndex() {
        long userId = -1L;
        int hash = Long.hashCode(userId) & Integer.MAX_VALUE;
        int expectedIndex = (hash / 2) % 16;

        String actual = algorithm.doSharding(
                availableTables,
                new PreciseShardingValue<>("t_order", "user_id", null, userId)
        );

        assertEquals("t_order_" + expectedIndex, actual);
    }

    private void assertRoute(long userId) {
        int hash = Long.hashCode(userId) & Integer.MAX_VALUE;
        int expectedIndex = (hash / 2) % 16;
        String actual = algorithm.doSharding(
                availableTables,
                new PreciseShardingValue<>("t_order", "user_id", null, userId)
        );
        assertEquals("t_order_" + expectedIndex, actual);
    }
}
