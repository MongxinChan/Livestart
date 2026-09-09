package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("t_refund_policy")
public class RefundPolicyDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long eventId;
    private Integer isAllowRefund;
    private Integer tier1DeadlineHours;
    private Integer tier2DeadlineHours;
    private BigDecimal tier2RefundFeeRate;
    private String policyDesc;
    private Date createTime;
    private Date updateTime;
}
