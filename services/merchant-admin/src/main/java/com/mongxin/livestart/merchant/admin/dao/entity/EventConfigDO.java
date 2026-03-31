package com.mongxin.livestart.merchant.admin.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("t_event_config")
public class EventConfigDO {

    /**
     * 对应演出ID (共享主键)
     */
    @TableId(type = IdType.INPUT)
    private Long eventId;

    /**
     * 选座模式 0:系统自动配座(高并发) 1:手动选座(剧场)
     */
    private Integer selectionMode;

    /**
     * 是否强制实名制入场 0:否 1:是
     */
    private Integer isVerifyRequired;

    /**
     * 单人账户最大购票上限
     */
    private Integer maxTicketsPerUser;

    /**
     * 退票政策 0:不可退 1:全额退 2:阶梯退票
     */
    private Integer refundPolicyType;

    /**
     * 全额退票截止时间(开演前X小时)
     */
    private Integer tier1FreeRefundHours;

    /**
     * 部分退票截止时间(开演前Y小时)
     */
    private Integer tier2PartialRefundHours;

    /**
     * 部分退票手续费比例 (0.20 代表20%)
     */
    private BigDecimal tier2RefundFeeRate;

    /**
     * 是否允许转赠门票 0:否 1:是
     */
    private Integer isTransferable;

    /**
     * 是否开启候补购票功能
     */
    private Integer isWaitingAllowed;
}
