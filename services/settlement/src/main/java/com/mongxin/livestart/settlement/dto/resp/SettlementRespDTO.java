package com.mongxin.livestart.settlement.dto.resp;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class SettlementRespDTO {

    private Long id;

    private Long eventId;

    private String eventTitle;

    private Integer totalTickets;

    private BigDecimal totalSalesAmount;

    private BigDecimal commissionRate;

    private BigDecimal commissionAmount;

    private BigDecimal settlementAmount;

    private Integer status;

    private Date createTime;

    private Date updateTime;
}
