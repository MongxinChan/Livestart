package com.mongxin.livestart.engine.dao.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * 订单表 Hash 取模分片算法（16 分表）
 * <p>
 * 分片列：user_id
 * 算法：hash(user_id) % 16 → t_order_{0..15}
 * <p>
 * 注意：Snowflake ID 的低12位是序列号（低并发下恒为0），
 * 直接取模会导致数据全部集中在 _0 分片。
 * 必须先用 Long.hashCode() 将高位时间戳混入低位再取模。
 */
public class OrderTableHashModShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private static final int SHARDING_COUNT = 16;

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {
        long userId = shardingValue.getValue();
        int hash = Long.hashCode(userId) & Integer.MAX_VALUE;
        int tableIndex = (hash / 2) % SHARDING_COUNT;
        String logicTableName = shardingValue.getLogicTableName();
        String targetTableName = logicTableName + "_" + tableIndex;
        for (String tableName : availableTargetNames) {
            if (tableName.endsWith("_" + tableIndex)) {
                return tableName;
            }
        }
        throw new IllegalArgumentException("未找到匹配的分片表: " + targetTableName);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Long> shardingValue) {
        // 范围查询：返回所有分片（不推荐，应避免使用）
        return availableTargetNames;
    }

    public Properties getProps() {
        return new Properties();
    }

    @Override
    public void init(Properties props) {
    }

    @Override
    public String getType() {
        return "CLASS_BASED";
    }
}
