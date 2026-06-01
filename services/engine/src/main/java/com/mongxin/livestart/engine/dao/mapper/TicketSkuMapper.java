package com.mongxin.livestart.engine.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.engine.dao.entity.TicketSkuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 票种 Mapper（操作公共库 live_start）
 */
@Mapper
public interface TicketSkuMapper extends BaseMapper<TicketSkuDO> {

    /**
     * 乐观锁扣减剩余库存（DB 二次校验）
     *
     * @param skuId   票种ID
     * @param count   扣减数量
     * @param version 当前版本号（乐观锁）
     * @return 影响行数，0 表示库存不足或版本冲突
     */
    @Update("UPDATE t_ticket_sku SET remaining_stock = remaining_stock - #{count}, version = version + 1 " +
            "WHERE id = #{skuId} AND remaining_stock >= #{count} AND version = #{version}")
    int decrementStock(@Param("skuId") Long skuId,
                       @Param("count") int count,
                       @Param("version") int version);

    /**
     * 归还库存（退票时使用）
     *
     * @param skuId 票种ID
     * @param count 归还数量
     * @return 影响行数
     */
    @Update("UPDATE t_ticket_sku SET remaining_stock = remaining_stock + #{count} WHERE id = #{skuId}")
    int returnStock(@Param("skuId") Long skuId, @Param("count") int count);
}
