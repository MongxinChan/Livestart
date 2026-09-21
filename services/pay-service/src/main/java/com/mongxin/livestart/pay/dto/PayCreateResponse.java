package com.mongxin.livestart.pay.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 支付创建结果，包含支付流水和支付宝页面内容
 */
@Data
@Builder
public class PayCreateResponse {

    /**
     * 平台支付流水号
     */
    private String paySn;

    /**
     * 业务订单号
     */
    private String orderNo;

    /**
     * 支付宝网页表单或跳转内容
     */
    private String body;

    /**
     * 当前支付状态
     */
    private Integer status;
}
