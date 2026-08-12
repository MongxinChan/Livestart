package com.mongxin.livestart.pay.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_pay")
public class PayDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String paySn;
    private String orderNo;
    private Long userId;
    private Integer channel;
    private Integer tradeType;
    private String subject;
    private String tradeNo;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer status;
    private Date gmtPayment;
    private Date createTime;
    private Date updateTime;
}
