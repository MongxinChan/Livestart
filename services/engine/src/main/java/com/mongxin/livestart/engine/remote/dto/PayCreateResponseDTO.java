package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

/** pay-service 返回的支付创建结果。 */
@Data
public class PayCreateResponseDTO {
    /** 平台支付流水号。 */
    private String paySn;
    /** 业务订单号。 */
    private String orderNo;
    /** 支付宝网页表单或跳转内容。 */
    private String body;
    /** 当前支付状态。 */
    private Integer status;
}
