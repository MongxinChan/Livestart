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
 * 艺人钱包不可变账本流水 DO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_artist_wallet_ledger")
public class ArtistWalletLedgerDO {

    /** 账本流水 ID。 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 艺人用户 ID。 */
    private Long artistId;

    /** 业务类型。 */
    private String bizType;

    /** 业务唯一号。 */
    private String bizNo;

    /** 方向：1增加，2减少。 */
    private Integer direction;

    /** 变动金额。 */
    private BigDecimal amount;

    /** 变动后的可用余额。 */
    private BigDecimal balanceAfter;

    /** 流水说明。 */
    private String remark;

    /** 创建时间。 */
    private Date createTime;
}
