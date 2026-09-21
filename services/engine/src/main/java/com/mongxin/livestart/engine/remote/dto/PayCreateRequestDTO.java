package com.mongxin.livestart.engine.remote.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** engine 调用 pay-service 创建支付的请求。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayCreateRequestDTO {
    /** 待支付的业务订单号。 */
    private String orderNo;
    /** 支付页面展示标题。 */
    private String subject;
}
