package com.mongxin.livestart.distribution.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 艺人钱包余额响应 DTO。
 */
@Data
@Schema(description = "艺人钱包余额")
public class ArtistWalletRespDTO {

    /** 艺人用户 ID。 */
    private Long artistId;

    /** 可提现余额。 */
    private BigDecimal availableAmount;

    /** 提现冻结金额。 */
    private BigDecimal frozenAmount;

    /** 累计入账金额。 */
    private BigDecimal totalEarned;

    /** 累计提现成功金额。 */
    private BigDecimal totalWithdrawn;
}
