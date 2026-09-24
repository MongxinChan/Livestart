package com.mongxin.livestart.distribution.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 艺人推广收益钱包账户 DO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_artist_wallet_account")
public class ArtistWalletAccountDO {

    /** 艺人用户 ID。 */
    @TableId
    private Long artistId;

    /** 当前可提现余额。 */
    private BigDecimal availableAmount;

    /** 已被提现申请冻结的余额。 */
    private BigDecimal frozenAmount;

    /** 累计入账金额。 */
    private BigDecimal totalEarned;

    /** 累计提现成功金额。 */
    private BigDecimal totalWithdrawn;

    /** 乐观锁版本号。 */
    private Integer version;

    /** 创建时间。 */
    private Date createTime;

    /** 修改时间。 */
    private Date updateTime;
}
