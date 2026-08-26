package com.mongxin.livestart.pay.dao.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PayShardingAlgorithmTest {
    private final PayShardingAlgorithm algorithm = new PayShardingAlgorithm();

    @Test
    void routesDatabaseToOneOfConfiguredDatabases() {
        String actual = algorithm.doSharding(List.of("ds_0", "ds_1"),
                new PreciseShardingValue<>("t_pay", "order_no", null, "ORDER-100"));
        assertTrue(actual.equals("ds_0") || actual.equals("ds_1"));
    }

    @Test
    void routesTableToConfiguredTableSuffix() {
        List<String> tables = IntStream.range(0, 16).mapToObj(i -> "t_pay_" + i).toList();
        String actual = algorithm.doSharding(tables,
                new PreciseShardingValue<>("t_pay", "order_no", null, "ORDER-100"));
        assertTrue(actual.matches("t_pay_(?:[0-9]|1[0-5])"));
    }
}
