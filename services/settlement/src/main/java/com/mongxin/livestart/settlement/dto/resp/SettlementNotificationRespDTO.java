package com.mongxin.livestart.settlement.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class SettlementNotificationRespDTO {

    private String notificationKey;

    private Long settlementId;

    private Long eventId;

    private String eventTitle;

    private String performerName;

    private Integer status;

    private String type;

    private String typeLabel;

    private String description;

    private BigDecimal settlementAmount;

    private BigDecimal commissionAmount;

    private Integer totalTickets;

    private Boolean read;

    private Date updateTime;
}
