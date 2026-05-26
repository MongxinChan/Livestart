package com.mongxin.livestart.distribution.dao.sharding;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;

import java.util.Collection;
import java.util.Properties;

/**
 * 门票表与只读订单表 Hash 取模分片算法（16 分表）
 * <p>
 * 分片列：user_id
 * 算法：user_id % 16 → t_user_ticket_{0..15} / t_order_{0..15}
 */
public class UserTicketTableHashModShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    private static final int SHARDING_COUNT = 16;

    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {
        long userId = shardingValue.getValue();
        int tableIndex = (int) (userId % SHARDING_COUNT);
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
        return availableTargetNames;
    }

    public Properties getProps() {
        return new Properties();
    }

    public void init(Properties props) {
    }

    public String getType() {
        return "CLASS_BASED";
    }
}
