package com.mongxin.livestart.distribution.dao.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserTicketTableHashModShardingAlgorithmTest {

    private final UserTicketTableHashModShardingAlgorithm algorithm = new UserTicketTableHashModShardingAlgorithm();
    private final List<String> availableTables = List.of(
            "t_user_ticket_0", "t_user_ticket_1", "t_user_ticket_2", "t_user_ticket_3",
            "t_user_ticket_4", "t_user_ticket_5", "t_user_ticket_6", "t_user_ticket_7",
            "t_user_ticket_8", "t_user_ticket_9", "t_user_ticket_10", "t_user_ticket_11",
            "t_user_ticket_12", "t_user_ticket_13", "t_user_ticket_14", "t_user_ticket_15"
    );

    @Test
    void shouldUsePositiveLongHashModuloForTableIndex() {
        long userId = -1L;
        int hash = Long.hashCode(userId) & Integer.MAX_VALUE;
        int expectedIndex = (hash / 2) % 16;

        String actual = algorithm.doSharding(
                availableTables,
                new PreciseShardingValue<>("t_user_ticket", "user_id", null, userId)
        );

        assertEquals("t_user_ticket_" + expectedIndex, actual);
    }

    @Test
    void shouldCoverAllPhysicalShardsWithDatabaseStrategy() {
        Set<String> physicalShards = new HashSet<>();

        for (long userId = 1; userId <= 4096; userId++) {
            int hash = Long.hashCode(userId) & Integer.MAX_VALUE;
            int databaseIndex = hash % 2;
            String tableName = algorithm.doSharding(
                    availableTables,
                    new PreciseShardingValue<>("t_user_ticket", "user_id", null, userId)
            );
            physicalShards.add("ds_order_" + databaseIndex + "." + tableName);
        }

        assertEquals(32, physicalShards.size());
    }
}
