package com.mongxin.livestart.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayCreateRequest {
    @NotBlank
    private String orderNo;
    private String subject;
}
