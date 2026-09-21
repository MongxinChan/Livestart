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

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联演出ID
     */
    private Long eventId;

    /**
     * 是否允许退票：0-否，1-是
     */
    private Integer isAllowRefund;

    /**
     * 一级退票截止时间，单位为小时
     */
    private Integer tier1DeadlineHours;

    /**
     * 二级退票截止时间，单位为小时
     */
    private Integer tier2DeadlineHours;

    /**
     * 二级退票手续费率
     */
    private BigDecimal tier2RefundFeeRate;

    /**
     * 退票政策说明
     */
    private String policyDesc;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
