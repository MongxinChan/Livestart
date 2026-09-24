package com.mongxin.livestart.engine.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 场馆权限范围内的一条成功核销记录。
 */
@Data
@Schema(description = "成功核销记录")
public class TicketVerifyRecordRespDTO {

    /** 电子票 ID。 */
    private Long id;

    /** 订单流水号。 */
    private String orderNo;

    /** 电子票核销码。 */
    private String checkCode;

    /** 演出 ID。 */
    private Long eventId;

    /** 核销时间。 */
    private Date checkedAt;

    /** 核销操作人的后台用户 ID。 */
    private Long checkedBy;
}
