package com.mongxin.livestart.pay.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建支付单并生成支付宝支付页面的请求
 */
@Data
public class PayCreateRequest {

    /**
     * 待支付的业务订单号
     */
    @NotBlank
    private String orderNo;

    /**
     * 支付页面展示标题，可选
     */
    private String subject;
}
