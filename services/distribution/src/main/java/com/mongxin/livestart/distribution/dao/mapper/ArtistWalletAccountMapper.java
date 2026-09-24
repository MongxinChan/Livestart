package com.mongxin.livestart.distribution.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mongxin.livestart.distribution.dao.entity.ArtistWalletAccountDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 艺人钱包账户 Mapper。
 */
@Mapper
public interface ArtistWalletAccountMapper extends BaseMapper<ArtistWalletAccountDO> {

    /** 增加可提现余额。 */
    @Update("UPDATE t_artist_wallet_account SET available_amount = available_amount + #{amount}, total_earned = total_earned + #{amount}, version = version + 1 WHERE artist_id = #{artistId}")
    int credit(@org.apache.ibatis.annotations.Param("artistId") Long artistId,
               @org.apache.ibatis.annotations.Param("amount") BigDecimal amount);

    /** 冻结可提现余额。 */
    @Update("UPDATE t_artist_wallet_account SET available_amount = available_amount - #{amount}, frozen_amount = frozen_amount + #{amount}, version = version + 1 WHERE artist_id = #{artistId} AND available_amount >= #{amount}")
    int freeze(@org.apache.ibatis.annotations.Param("artistId") Long artistId,
               @org.apache.ibatis.annotations.Param("amount") BigDecimal amount);

    /** 取消提现并释放冻结余额。 */
    @Update("UPDATE t_artist_wallet_account SET available_amount = available_amount + #{amount}, frozen_amount = frozen_amount - #{amount}, version = version + 1 WHERE artist_id = #{artistId} AND frozen_amount >= #{amount}")
    int release(@org.apache.ibatis.annotations.Param("artistId") Long artistId,
                @org.apache.ibatis.annotations.Param("amount") BigDecimal amount);

    /** 提现完成后扣除冻结余额。 */
    @Update("UPDATE t_artist_wallet_account SET frozen_amount = frozen_amount - #{amount}, total_withdrawn = total_withdrawn + #{amount}, version = version + 1 WHERE artist_id = #{artistId} AND frozen_amount >= #{amount}")
    int completeWithdrawal(@org.apache.ibatis.annotations.Param("artistId") Long artistId,
                           @org.apache.ibatis.annotations.Param("amount") BigDecimal amount);
}
