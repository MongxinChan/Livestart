package com.mongxin.livestart.pay.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.pay.dao.entity.PayOutboxDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface PayOutboxMapper extends BaseMapper<PayOutboxDO> {
    @Select("SELECT * FROM t_pay_outbox WHERE status = 0 AND (next_retry_time IS NULL OR next_retry_time <= NOW()) ORDER BY id LIMIT 100")
    List<PayOutboxDO> selectPending();

    @Update("UPDATE t_pay_outbox SET status = 1, update_time = NOW() WHERE id = #{id} AND status = 0")
    int markSent(@Param("id") Long id);

    @Update("UPDATE t_pay_outbox SET retry_count = retry_count + 1, next_retry_time = #{nextRetryTime}, update_time = NOW() WHERE id = #{id} AND status = 0")
    int scheduleRetry(@Param("id") Long id, @Param("nextRetryTime") Date nextRetryTime);
}
