package com.mongxin.livestart.pay.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 退款单，保存一次业务订单退款申请及支付宝退款结果
 */
@Data
@TableName("t_refund")
public class RefundDO {

    /**
     * 退款单主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 平台退款请求号，也是支付宝的商户退款请求号
     */
    private String refundNo;

    /**
     * 业务订单号
     */
    private String orderNo;

    /**
     * 平台支付流水号
     */
    private String paySn;

    /**
     * 原支付宝交易号
     */
    private String tradeNo;

    /**
     * 支付宝返回的退款交易号
     */
    private String refundTradeNo;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

    /**
     * 用户填写的退款原因
     */
    private String reason;

    /**
     * 退款状态，0 处理中，1 成功
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新时间
     */
    private Date updateTime;
}
