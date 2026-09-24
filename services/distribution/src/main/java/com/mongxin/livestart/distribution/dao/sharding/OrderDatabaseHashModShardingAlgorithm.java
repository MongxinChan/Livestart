package com.mongxin.livestart.distribution.dao.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * 订单库 Hash 取模分片算法（2 库）。
 *
 * <p>使用 Java 实现替代 Inline Groovy 表达式，避免 JDK 版本变化影响分片路由。</p>
 */
public class OrderDatabaseHashModShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private static final int DATABASE_COUNT = 2;

    @Override
    public String doSharding(Collection<String> availableTargetNames,
                              PreciseShardingValue<Long> shardingValue) {
        int databaseIndex = Math.floorMod(Long.hashCode(shardingValue.getValue()), DATABASE_COUNT);
        return availableTargetNames.stream()
                .filter(name -> name.endsWith("_" + databaseIndex))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到订单库分片: " + databaseIndex));
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<Long> shardingValue) {
        return availableTargetNames;
    }

    @Override
    public void init(Properties props) {
    }

    @Override
    public String getType() {
        return "CLASS_BASED";
    }
}
