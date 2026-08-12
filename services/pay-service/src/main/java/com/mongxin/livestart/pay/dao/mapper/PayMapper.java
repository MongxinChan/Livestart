package com.mongxin.livestart.pay.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.pay.dao.entity.PayDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.Date;

@Mapper
public interface PayMapper extends BaseMapper<PayDO> {

    @Update("UPDATE t_pay SET status = #{targetStatus}, trade_no = #{tradeNo}, pay_amount = #{payAmount}, gmt_payment = #{gmtPayment}, update_time = NOW() WHERE order_no = #{orderNo} AND status = #{expectedStatus}")
    int markSuccessIfPending(@Param("orderNo") String orderNo,
                             @Param("targetStatus") int targetStatus,
                             @Param("expectedStatus") int expectedStatus,
                             @Param("tradeNo") String tradeNo,
                             @Param("payAmount") BigDecimal payAmount,
                             @Param("gmtPayment") Date gmtPayment);
}
