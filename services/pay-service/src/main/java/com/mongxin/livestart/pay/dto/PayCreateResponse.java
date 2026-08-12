package com.mongxin.livestart.pay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayCreateResponse {
    private String paySn;
    private String orderNo;
    private String body;
    private Integer status;
}
