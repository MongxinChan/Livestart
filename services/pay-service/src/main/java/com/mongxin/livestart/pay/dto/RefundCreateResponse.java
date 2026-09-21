package com.mongxin.livestart.pay.dto;

import lombok.Builder;
import lombok.Data;

/** 退款创建结果。 */
@Data
@Builder
public class RefundCreateResponse {
    /** 平台退款请求号。 */
    private String refundNo;
    /** 业务订单号。 */
    private String orderNo;
    /** 退款状态，0 处理中，1 成功。 */
    private Integer status;
}
