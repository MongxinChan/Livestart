package com.mongxin.livestart.admin.dao.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * 用户数据的 2 库 8 表路由，使用不同哈希位覆盖 16 个物理分片。
 */
public class UserHashModShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private static final int DATABASE_COUNT = 2;
    private static final int TABLE_COUNT = 8;

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {
        int hash = Long.hashCode(shardingValue.getValue()) & Integer.MAX_VALUE;
        int databaseIndex = hash % DATABASE_COUNT;
        int tableIndex = (hash / DATABASE_COUNT) % TABLE_COUNT;
        boolean databaseRoute = availableTargetNames.stream().anyMatch(name -> name.startsWith("ds_user_"));
        int targetIndex = databaseRoute ? databaseIndex : tableIndex;
        return availableTargetNames.stream()
                .filter(name -> name.endsWith("_" + targetIndex))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "未找到用户分片: " + shardingValue.getLogicTableName() + "_" + targetIndex));
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
