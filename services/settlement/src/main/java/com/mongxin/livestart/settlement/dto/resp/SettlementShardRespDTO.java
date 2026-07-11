package com.mongxin.livestart.settlement.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 分表结算汇总明细响应 DTO
 */
@Data
@Builder
public class SettlementShardRespDTO {

    /**
     * 分表分片索引
     */
    private Integer shardIndex;

    /**
     * 物理数据库表名 (如 t_order_0)
     */
    private String tableName;

    /**
     * 物理分片表关联的出票总数
     */
    private Integer totalTickets;

    /**
     * 物理分片表关联的销售额
     */
    private BigDecimal totalSalesAmount;

    /**
     * 物理分片表关联的佣金金额
     */
    private BigDecimal commissionAmount;

    /**
     * 物理分片表关联的结算金额
     */
    private BigDecimal settlementAmount;
}
