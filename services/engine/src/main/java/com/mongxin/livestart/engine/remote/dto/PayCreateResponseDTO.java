package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

@Data
public class PayCreateResponseDTO {
    private String paySn;
    private String orderNo;
    private String body;
    private Integer status;
}
