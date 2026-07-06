package com.mongxin.livestart.engine.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

/**
 * 订单 Mapper
 */
@Mapper
public interface OrderMapper extends BaseMapper<OrderDO> {

    /**
     * 更新订单状态（CAS 风格，防并发）
     *
     * @param id            订单ID
     * @param userId        用户ID（分片键）
     * @param targetStatus  目标状态
     * @param expectedStatus 期望当前状态
     * @return 影响行数
     */
    @Update("UPDATE t_order SET status = #{targetStatus} WHERE id = #{id} AND user_id = #{userId} AND status = #{expectedStatus}")
    int updateOrderStatus(@Param("id") Long id,
                          @Param("userId") Long userId,
                          @Param("targetStatus") int targetStatus,
                          @Param("expectedStatus") int expectedStatus);

    @Update("UPDATE t_order SET pay_time = #{payTime} WHERE id = #{id} AND user_id = #{userId}")
    int updatePayTime(@Param("id") Long id,
                      @Param("userId") Long userId,
                      @Param("payTime") Date payTime);

    @Select({
            "<script>",
            "SELECT o.* FROM t_order o",
            "<if test='eventIds != null and eventIds.size() > 0'>",
            "JOIN t_order_item oi",
            "  ON oi.order_no = o.order_no",
            " AND oi.user_id = o.user_id",
            " AND oi.event_id IN",
            " <foreach collection='eventIds' item='eventId' open='(' separator=',' close=')'>",
            "   #{eventId}",
            " </foreach>",
            "</if>",
            "WHERE 1 = 1",
            "<if test='status != null'>",
            "  AND o.status = #{status}",
            "</if>",
            "<if test='eventIds != null and eventIds.size() > 0'>",
            "GROUP BY o.id, o.order_no, o.user_id, o.total_amount, o.status, o.pay_time, o.create_time",
            "</if>",
            "ORDER BY o.create_time DESC",
            "</script>"
    })
    Page<OrderDO> pageQueryAdminOrders(Page<OrderDO> page,
                                       @Param("status") Integer status,
                                       @Param("eventIds") List<Long> eventIds);
}
