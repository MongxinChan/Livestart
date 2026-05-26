package com.mongxin.livestart.distribution.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 艺人推广专属码及分成收益统计响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeRespDTO {

    /**
     * 艺人用户ID
     */
    private Long userId;

    /**
     * 艺人专属推广宣发码
     */
    private String inviteCode;

    /**
     * 推广绑定的歌迷累计数量
     */
    private Integer inviteeCount;

    /**
     * 已结算到账的净收益 (税后已扣除20%劳务税)
     */
    private BigDecimal settledCommission;

    /**
     * 待结算的净收益 (税后已核算，处于15天在途状态)
     */
    private BigDecimal pendingCommission;
}
