package com.mongxin.livestart.pay.remote.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayableOrderDTO {
    private String orderNo;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;
}
