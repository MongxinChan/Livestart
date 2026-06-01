package com.mongxin.livestart.settlement.dto.resp;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class SettlementStatsRespDTO {

    /**
     * 结算演出总数
     */
    private Integer totalEvents;

    /**
     * 总出票数
     */
    private Integer totalTickets;

    /**
     * 总销售额 (票房总收入)
     */
    private BigDecimal grossRevenue;

    /**
     * 总抽成佣金
     */
    private BigDecimal totalCommission;

    /**
     * 商家净应结总额
     */
    private BigDecimal netSettlement;
}
