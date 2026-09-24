package com.mongxin.livestart.distribution.dto.resp;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 艺人提现申请响应 DTO。
 */
@Data
public class ArtistWithdrawalRespDTO {

    /** 提现申请 ID。 */
    private Long id;

    /** 提现请求幂等号。 */
    private String requestNo;

    /** 提现金额。 */
    private BigDecimal amount;

    /** 状态：0待审核，1处理中，2已完成，3已拒绝，4已取消。 */
    private Integer status;

    /** 收款账户类型。 */
    private String accountType;

    /** 收款账号快照。 */
    private String accountNo;

    /** 收款人姓名快照。 */
    private String accountName;

    /** 外部支付流水号。 */
    private String externalNo;

    /** 审核说明。 */
    private String auditRemark;

    /** 创建时间。 */
    private Date createTime;

    /** 完成时间。 */
    private Date completedTime;
}
