package com.mongxin.livestart.pay.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_pay_outbox")
public class PayOutboxDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String eventId;
    private String aggregateId;
    private String eventType;
    private String payload;
    private Integer status;
    private Integer retryCount;
    private Date nextRetryTime;
    private Date createTime;
    private Date updateTime;
}
