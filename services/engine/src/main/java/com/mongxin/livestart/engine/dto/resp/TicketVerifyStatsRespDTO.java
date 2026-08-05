package com.mongxin.livestart.engine.dto.resp;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现场验票统计返回对象
 */
@Data
public class TicketVerifyStatsRespDTO {

    /**
     * 电子票总数
     */
    private Long totalCount;

    /**
     * 已验票数
     */
    private Long checkedCount;

    /**
     * 未验票数
     */
    private Long uncheckedCount;

    /**
     * 入场率，单位：百分比
     */
    private BigDecimal checkedRate;
}
