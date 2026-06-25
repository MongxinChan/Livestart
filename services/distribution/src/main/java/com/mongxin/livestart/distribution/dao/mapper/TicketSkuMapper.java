package com.mongxin.livestart.distribution.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 门票票档库存 Mapper 接口
 */
@Mapper
public interface TicketSkuMapper extends BaseMapper<TicketSkuDO> {

    @Update("UPDATE t_ticket_sku SET remaining_stock = remaining_stock - #{count}, version = version + 1 " +
            "WHERE id = #{skuId} AND remaining_stock >= #{count} AND version = #{version}")
    int decrementStock(@Param("skuId") Long skuId,
                       @Param("count") int count,
                       @Param("version") int version);
}
