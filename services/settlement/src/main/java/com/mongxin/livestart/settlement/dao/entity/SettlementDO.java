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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_settlement")
public class SettlementDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long eventId;

    private String eventTitle;

    private Integer totalTickets;

    private BigDecimal totalSalesAmount;

    private BigDecimal commissionRate;

    private BigDecimal commissionAmount;

    private BigDecimal settlementAmount;

    /**
     * 结算状态 0:未结算 1:已结算 2:结算异常
     */
    private Integer status;

    /**
     * 结算异常信息
     */
    private String errorMessage;

    private Date createTime;

    private Date updateTime;
}
