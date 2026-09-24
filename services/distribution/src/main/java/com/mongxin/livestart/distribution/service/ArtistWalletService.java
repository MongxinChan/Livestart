package com.mongxin.livestart.distribution.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.distribution.dto.req.ArtistWithdrawalCreateReqDTO;
import com.mongxin.livestart.distribution.dto.resp.ArtistWalletRespDTO;
import com.mongxin.livestart.distribution.dto.resp.ArtistWithdrawalRespDTO;

/**
 * 艺人收益钱包与提现服务。
 */
public interface ArtistWalletService {

    /** 查询当前艺人钱包。 */
    ArtistWalletRespDTO getCurrentWallet();

    /** 申请提现并冻结可提现余额。 */
    ArtistWithdrawalRespDTO createWithdrawal(ArtistWithdrawalCreateReqDTO request);

    /** 查询当前艺人的提现申请。 */
    IPage<ArtistWithdrawalRespDTO> pageWithdrawals(int pageNo, int pageSize);

    /** 取消待审核提现并释放冻结余额。 */
    void cancelWithdrawal(Long withdrawalId);

    /** 管理员完成提现打款。 */
    void completeWithdrawal(Long withdrawalId, String externalNo);

    /** 佣金到账钱包，使用 commissionRecordId 保证幂等。 */
    void creditCommission(Long artistId, Long commissionRecordId, java.math.BigDecimal amount);
}
