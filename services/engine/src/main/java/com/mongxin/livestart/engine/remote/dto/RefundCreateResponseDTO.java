package com.mongxin.livestart.engine.remote.dto;

import lombok.Data;

/**
 * pay-service 返回的退款创建结果
 */
@Data
public class RefundCreateResponseDTO {

    /**
     * 平台退款请求号
     */
    private String refundNo;

    /**
     * 业务订单号
     */
    private String orderNo;

    /**
     * 退款状态，0 处理中，1 成功
     */
    private Integer status;
}
