package com.mongxin.livestart.pay.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RefundCreateResponse {
    private String refundNo;
    private String orderNo;
    private Integer status;
}
