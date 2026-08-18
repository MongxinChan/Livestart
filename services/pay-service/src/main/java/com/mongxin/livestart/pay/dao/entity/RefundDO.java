package com.mongxin.livestart.pay.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_refund")
public class RefundDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String refundNo;
    private String orderNo;
    private String paySn;
    private String tradeNo;
    private String refundTradeNo;
    private BigDecimal refundAmount;
    private String reason;
    private Integer status;
    private Date createTime;
    private Date updateTime;
}
