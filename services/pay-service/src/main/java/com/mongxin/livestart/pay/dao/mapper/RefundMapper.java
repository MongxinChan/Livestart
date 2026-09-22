package com.mongxin.livestart.pay.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.pay.dao.entity.RefundDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface RefundMapper extends BaseMapper<RefundDO> {

    @Select("SELECT * FROM t_refund WHERE status = 0 ORDER BY update_time ASC LIMIT 100")
    List<RefundDO> selectPending();

    @Update("UPDATE t_refund SET status = #{targetStatus}, refund_trade_no = #{refundTradeNo}, "
            + "update_time = #{updateTime} WHERE order_no = #{orderNo} AND refund_no = #{refundNo} "
            + "AND status = #{expectedStatus}")
    int markSuccessIfPending(@Param("orderNo") String orderNo,
                             @Param("refundNo") String refundNo,
                             @Param("targetStatus") int targetStatus,
                             @Param("expectedStatus") int expectedStatus,
                             @Param("refundTradeNo") String refundTradeNo,
                             @Param("updateTime") Date updateTime);
}
