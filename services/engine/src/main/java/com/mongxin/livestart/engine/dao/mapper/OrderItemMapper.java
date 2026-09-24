package com.mongxin.livestart.engine.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细 Mapper
 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemDO> {

    @Select({
            "<script>",
            "SELECT COUNT(*) FROM t_order_item oi",
            "JOIN t_order o ON o.order_no = oi.order_no AND o.user_id = oi.user_id",
            "WHERE o.status = 1",
            "<if test='checked'> AND oi.is_checked = 1 </if>",
            "<if test='eventIds != null and eventIds.size() > 0'>",
            "AND oi.event_id IN",
            "<foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>",
            "#{eventId}",
            "</foreach>",
            "</if>",
            "</script>"
    })
    Long countPaidTickets(@Param("eventIds") List<Long> eventIds, @Param("checked") boolean checked);
}
