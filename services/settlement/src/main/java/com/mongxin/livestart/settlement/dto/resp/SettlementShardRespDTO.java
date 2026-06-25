package com.mongxin.livestart.settlement.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class SettlementShardRespDTO {

    private Integer shardIndex;

    private String tableName;

    private Integer totalTickets;

    private BigDecimal totalSalesAmount;

    private BigDecimal commissionAmount;

    private BigDecimal settlementAmount;
}
