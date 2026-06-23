package com.mongxin.livestart.merchant.admin.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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
    @Update("UPDATE t_ticket_sku SET total_stock = total_stock + #{count}, remaining_stock = remaining_stock + #{count} WHERE id = #{skuId}")
    int increaseStock(@Param("skuId") Long skuId, @Param("count") Integer count);

    @Select("SELECT id,event_id,title,original_price,selling_price,total_stock,stage1_stock,stage2_stock,stage2_released,remaining_stock,limit_num,version " +
            "FROM t_ticket_sku WHERE event_id = #{eventId}")
    List<TicketSkuDO> selectByEventId(@Param("eventId") Long eventId);

    @Update("UPDATE t_ticket_sku " +
            "SET remaining_stock = remaining_stock + stage2_stock, stage2_released = 1 " +
            "WHERE event_id = #{eventId} AND IFNULL(stage2_stock, 0) > 0 AND IFNULL(stage2_released, 0) = 0")
    int releaseStage2StockByEventId(@Param("eventId") Long eventId);
}
