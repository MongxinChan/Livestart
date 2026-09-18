package com.mongxin.livestart.engine.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 退款后的库存回补任务。任务落在公共库，数据库库存和任务状态可以在同一事务内更新。
 */
@Data
@TableName("t_stock_restore_task")
public class StockRestoreTaskDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String bizType;
    private String orderNo;
    private Long userId;
    private Long eventId;
    private Long skuId;
    private Integer restoreCount;
    private Integer refundConfirmed;
    private Integer dbRestored;
    private Integer redisRestored;
    private Integer status;
    private Integer retryCount;
    private Date nextRetryTime;
    private String lastError;
    private Date createTime;
    private Date updateTime;
}
