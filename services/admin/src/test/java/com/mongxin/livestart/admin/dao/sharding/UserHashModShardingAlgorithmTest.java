package com.mongxin.livestart.admin.dao.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserHashModShardingAlgorithmTest {

    private final UserHashModShardingAlgorithm algorithm = new UserHashModShardingAlgorithm();

    @Test
    void shouldUseSeparateHashBitsForDatabaseAndTable() {
        long userId = 2072622373967400961L;
        int hash = Long.hashCode(userId) & Integer.MAX_VALUE;

        String database = algorithm.doSharding(
                List.of("ds_user_0", "ds_user_1"),
                new PreciseShardingValue<>("t_user", "id", null, userId));
        String table = algorithm.doSharding(
                List.of("t_user_0", "t_user_1", "t_user_2", "t_user_3",
                        "t_user_4", "t_user_5", "t_user_6", "t_user_7"),
                new PreciseShardingValue<>("t_user", "id", null, userId));

        assertEquals("ds_user_" + hash % 2, database);
        assertEquals("t_user_" + (hash / 2) % 8, table);
    }

    @Test
    void shouldSupportProfileAndVisitorLogicalTables() {
        long userId = 1008L;
        int hash = Long.hashCode(userId) & Integer.MAX_VALUE;

        assertEquals("t_user_profile_" + (hash / 2) % 8,
                algorithm.doSharding(List.of("t_user_profile_0", "t_user_profile_1", "t_user_profile_2",
                                "t_user_profile_3", "t_user_profile_4", "t_user_profile_5",
                                "t_user_profile_6", "t_user_profile_7"),
                        new PreciseShardingValue<>("t_user_profile", "user_id", null, userId)));
    }
}
