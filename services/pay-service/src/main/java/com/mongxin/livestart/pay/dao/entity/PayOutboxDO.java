package com.mongxin.livestart.pay.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 支付成功事件 Outbox，保证支付单更新与消息投递最终一致
 */
@Data
@TableName("t_pay_outbox")
public class PayOutboxDO {

    /**
     * Outbox 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 事件唯一 ID
     */
    private String eventId;

    /**
     * 事件聚合根 ID，通常为订单号
     */
    private String aggregateId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件 JSON 载荷
     */
    private String payload;

    /**
     * 投递状态，0 待投递，1 已投递
     */
    private Integer status;

    /**
     * 已重试次数
     */
    private Integer retryCount;

    /**
     * 下一次允许投递的时间
     */
    private Date nextRetryTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date updateTime;
}
