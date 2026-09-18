package com.mongxin.livestart.engine.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.engine.dao.entity.StockRestoreTaskDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;
import java.util.List;

@Mapper
public interface StockRestoreTaskMapper extends BaseMapper<StockRestoreTaskDO> {

    @Select("SELECT * FROM t_stock_restore_task "
            + "WHERE status = 0 AND (next_retry_time IS NULL OR next_retry_time <= NOW()) "
            + "ORDER BY id LIMIT 100")
    List<StockRestoreTaskDO> selectPending();

    @Select("SELECT * FROM t_stock_restore_task WHERE id = #{id} FOR UPDATE")
    StockRestoreTaskDO selectForUpdate(@Param("id") Long id);

    @Update("UPDATE t_stock_restore_task SET db_restored = 1, update_time = NOW() "
            + "WHERE id = #{id} AND status = 0 AND db_restored = 0")
    int markDbRestored(@Param("id") Long id);

    @Update("UPDATE t_stock_restore_task SET refund_confirmed = 1, update_time = NOW() "
            + "WHERE id = #{id} AND status = 0 AND refund_confirmed = 0")
    int markRefundConfirmed(@Param("id") Long id);

    @Update("UPDATE t_stock_restore_task SET redis_restored = 1, update_time = NOW() "
            + "WHERE id = #{id} AND status = 0 AND redis_restored = 0")
    int markRedisRestored(@Param("id") Long id);

    @Update("UPDATE t_stock_restore_task SET status = 1, update_time = NOW() "
            + "WHERE id = #{id} AND status = 0 AND db_restored = 1 AND redis_restored = 1")
    int markCompleted(@Param("id") Long id);

    @Update("UPDATE t_stock_restore_task SET retry_count = retry_count + 1, "
            + "next_retry_time = #{nextRetryTime}, last_error = #{lastError}, update_time = NOW() "
            + "WHERE id = #{id} AND status = 0")
    int scheduleRetry(@Param("id") Long id,
                      @Param("nextRetryTime") Date nextRetryTime,
                      @Param("lastError") String lastError);
}
