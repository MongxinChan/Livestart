package com.mongxin.livestart.distribution.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 艺人提现申请 DO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_artist_withdrawal")
public class ArtistWithdrawalDO {

    /** 提现申请 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 提现请求幂等号。 */
    private String requestNo;

    /** 艺人用户 ID。 */
    private Long artistId;

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

    /** 修改时间。 */
    private Date updateTime;

    /** 完成时间。 */
    private Date completedTime;
}
