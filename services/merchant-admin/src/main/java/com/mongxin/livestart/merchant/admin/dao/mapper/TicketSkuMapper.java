package com.mongxin.livestart.merchant.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 票种/档位持久层映射器
 */
@Mapper
public interface TicketSkuMapper extends BaseMapper<TicketSkuDO> {

    /**
     * 原子增发库存（数据库层面保证并发安全）
     *
     * @param skuId 票种ID
     * @param count 增发数量
     * @return 影响行数
     */
    @Update("UPDATE ticket_skus SET total_stock = total_stock + #{count}, remaining_stock = remaining_stock + #{count} WHERE id = #{skuId}")
    int increaseStock(@Param("skuId") Long skuId, @Param("count") Integer count);
}
