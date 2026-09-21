package com.mongxin.livestart.pay.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/** 支付单，记录业务订单与支付宝交易的映射及支付金额状态。 */
@Data
@TableName("t_pay")
public class PayDO {
    /** 支付单主键。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    /** 平台支付流水号。 */
    private String paySn;
    /** 业务订单号。 */
    private String orderNo;
    /** 支付用户 ID。 */
    private Long userId;
    /** 支付渠道，例如支付宝。 */
    private Integer channel;
    /** 交易类型，例如网页支付。 */
    private Integer tradeType;
    /** 支付页面展示标题。 */
    private String subject;
    /** 支付宝交易号。 */
    private String tradeNo;
    /** 创建支付单时的应付金额。 */
    private BigDecimal totalAmount;
    /** 支付平台确认的实际支付金额。 */
    private BigDecimal payAmount;
    /** 支付状态，取值见 {@code PayStatus}。 */
    private Integer status;
    /** 支付宝确认支付时间。 */
    private Date gmtPayment;
    /** 创建时间。 */
    private Date createTime;
    /** 最后更新时间。 */
    private Date updateTime;
}
