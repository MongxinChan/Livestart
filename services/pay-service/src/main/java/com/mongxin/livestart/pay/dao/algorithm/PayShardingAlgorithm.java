package com.mongxin.livestart.pay.dao.algorithm;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * order_no/pay_sn 的稳定路由：32 个物理分片，前 16 个位于 ds_0，后 16 个位于 ds_1。
 */
public class PayShardingAlgorithm implements StandardShardingAlgorithm<String> {
    @Override
    public String doSharding(Collection<String> availableTargetNames,
                             PreciseShardingValue<String> shardingValue) {
        int index = Math.floorMod(shardingValue.getValue().hashCode(), 32);
        int databaseIndex = index / 16;
        int tableIndex = index % 16;
        boolean databaseRoute = availableTargetNames.stream().anyMatch(name -> name.startsWith("ds_"));
        String target = availableTargetNames.stream()
                .filter(name -> databaseRoute
                        ? name.endsWith("_" + databaseIndex)
                        : name.endsWith("_" + tableIndex))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No pay shard for index " + index));
        return target;
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames,
                                         RangeShardingValue<String> shardingValue) {
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
