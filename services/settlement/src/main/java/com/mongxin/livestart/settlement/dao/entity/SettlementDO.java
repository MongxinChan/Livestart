package com.mongxin.livestart.settlement.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 演出结算账单持久层实体，对应表：t_settlement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_settlement")
public class SettlementDO {

    /**
     * 结算记录ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 演出活动ID
     */
    private Long eventId;

    /**
     * 演出名称
     */
    private String eventTitle;

    /**
     * 本场演出总出票数
     */
    private Integer totalTickets;

    /**
     * 总销售票房金额
     */
    private BigDecimal totalSalesAmount;

    /**
     * 佣金比例 (例如 0.10 代表 10%)
     */
    private BigDecimal commissionRate;

    /**
     * 佣金抽成金额
     */
    private BigDecimal commissionAmount;

    /**
     * 主办方应结净额 (总销售额 - 佣金)
     */
    private BigDecimal settlementAmount;

    /**
     * 结算状态 0:未结算 1:已结算 2:结算异常
     */
    private Integer status;

    /**
     * 结算异常信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;
}
