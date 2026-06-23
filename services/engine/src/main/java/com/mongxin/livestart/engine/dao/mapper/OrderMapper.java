package com.mongxin.livestart.engine.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

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
}
