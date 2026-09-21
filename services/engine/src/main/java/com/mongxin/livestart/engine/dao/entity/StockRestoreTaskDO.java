package com.mongxin.livestart.engine.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 退款后的库存回补任务。任务落在公共库，数据库库存和任务状态可以在同一事务内更新。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_stock_restore_task")
public class StockRestoreTaskDO {

    /**
     * 任务主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 业务类型，目前固定为 REFUND
     */
    private String bizType;

    /**
     * 业务订单号，和退款单按业务维度幂等
     */
    private String orderNo;

    /**
     * 下单用户 ID
     */
    private Long userId;

    /**
     * 演出 ID
     */
    private Long eventId;

    /**
     * 票档 ID
     */
    private Long skuId;

    /**
     * 需要回补的票数
     */
    private Integer restoreCount;

    /**
     * 支付服务是否已经确认退款成功，0 否，1 是
     */
    private Integer refundConfirmed;

    /**
     * 数据库库存是否已回补，0 否，1 是
     */
    private Integer dbRestored;

    /**
     * Redis 库存是否已回补，0 否，1 是
     */
    private Integer redisRestored;

    /**
     * 任务状态，0 待处理，1 已完成
     */
    private Integer status;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 下一次允许重试的时间
     */
    private Date nextRetryTime;

    /**
     * 最近一次失败原因
     */
    private String lastError;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date updateTime;
}
