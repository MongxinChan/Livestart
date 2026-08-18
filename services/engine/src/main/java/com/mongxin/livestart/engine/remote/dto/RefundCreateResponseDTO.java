package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

@Data
public class RefundCreateResponseDTO {
    private String refundNo;
    private String orderNo;
    private Integer status;
}
