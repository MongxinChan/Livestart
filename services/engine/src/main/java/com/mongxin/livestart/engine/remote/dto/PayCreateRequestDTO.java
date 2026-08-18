package com.mongxin.livestart.engine.remote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCreateRequestDTO {
    private String orderNo;
    private String subject;
}
