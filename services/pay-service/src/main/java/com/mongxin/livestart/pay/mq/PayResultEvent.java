package com.mongxin.livestart.pay.mq;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@Builder
public class PayResultEvent {
    private String eventId;
    private String paySn;
    private String orderNo;
    private Long userId;
    private String tradeNo;
    private BigDecimal payAmount;
    private Date paidAt;
}
