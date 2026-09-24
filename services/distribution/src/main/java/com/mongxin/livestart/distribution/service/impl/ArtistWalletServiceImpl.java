package com.mongxin.livestart.distribution.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.distribution.common.biz.user.UserContext;
import com.mongxin.livestart.distribution.dao.entity.ArtistWalletAccountDO;
import com.mongxin.livestart.distribution.dao.entity.ArtistWalletLedgerDO;
import com.mongxin.livestart.distribution.dao.entity.ArtistWithdrawalDO;
import com.mongxin.livestart.distribution.dao.mapper.ArtistWalletAccountMapper;
import com.mongxin.livestart.distribution.dao.mapper.ArtistWalletLedgerMapper;
import com.mongxin.livestart.distribution.dao.mapper.ArtistWithdrawalMapper;
import com.mongxin.livestart.distribution.dto.req.ArtistWithdrawalCreateReqDTO;
import com.mongxin.livestart.distribution.dto.resp.ArtistWalletRespDTO;
import com.mongxin.livestart.distribution.dto.resp.ArtistWithdrawalRespDTO;
import com.mongxin.livestart.distribution.service.ArtistWalletService;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 艺人收益钱包与提现服务实现。
 */
@Service
@RequiredArgsConstructor
public class ArtistWalletServiceImpl implements ArtistWalletService {

    private static final int ARTIST_USER_TYPE = 2;
    private static final int SUPER_ADMIN_USER_TYPE = 4;
    private static final int LEDGER_CREDIT = 1;
    private static final int LEDGER_DEBIT = 2;

    private final ArtistWalletAccountMapper accountMapper;
    private final ArtistWalletLedgerMapper ledgerMapper;
    private final ArtistWithdrawalMapper withdrawalMapper;
    private final RedissonClient redissonClient;

    @Override
    public ArtistWalletRespDTO getCurrentWallet() {
        Long artistId = requireArtistId();
        ArtistWalletAccountDO account = getOrCreateAccount(artistId);
        ArtistWalletRespDTO result = new ArtistWalletRespDTO();
        BeanUtils.copyProperties(account, result);
        result.setArtistId(account.getArtistId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArtistWithdrawalRespDTO createWithdrawal(ArtistWithdrawalCreateReqDTO request) {
        Long artistId = requireArtistId();
        BigDecimal amount = normalizeAmount(request.getAmount());
        String requestNo = request.getRequestNo().trim();
        String accountType = request.getAccountType().trim();
        String accountNo = request.getAccountNo().trim();
        String accountName = request.getAccountName();
        ArtistWithdrawalDO existing = withdrawalMapper.selectOne(Wrappers.lambdaQuery(ArtistWithdrawalDO.class)
                .eq(ArtistWithdrawalDO::getRequestNo, requestNo));
        if (existing != null) {
            ensureSameWithdrawal(existing, artistId, amount, accountType, accountNo, accountName);
            return toResponse(existing);
        }

        RLock lock = redissonClient.getLock("livestart:artist:wallet:withdraw:" + artistId);
        lock.lock();
        try {
            existing = withdrawalMapper.selectOne(Wrappers.lambdaQuery(ArtistWithdrawalDO.class)
                    .eq(ArtistWithdrawalDO::getRequestNo, requestNo));
            if (existing != null) {
                ensureSameWithdrawal(existing, artistId, amount, accountType, accountNo, accountName);
                return toResponse(existing);
            }

            getOrCreateAccount(artistId);
            if (accountMapper.freeze(artistId, amount) <= 0) {
                throw new ClientException("可提现余额不足");
            }

            ArtistWithdrawalDO withdrawal = ArtistWithdrawalDO.builder()
                    .requestNo(requestNo)
                    .artistId(artistId)
                    .amount(amount)
                    .status(0)
                    .accountType(accountType)
                    .accountNo(accountNo)
                    .accountName(accountName)
                    .build();
            if (withdrawalMapper.insert(withdrawal) <= 0) {
                throw new ServiceException("提现申请创建失败");
            }
            insertLedger(artistId, "WITHDRAW_FREEZE", requestNo, LEDGER_DEBIT, amount,
                    getOrCreateAccount(artistId).getAvailableAmount(), "提现冻结");
            return toResponse(withdrawal);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public IPage<ArtistWithdrawalRespDTO> pageWithdrawals(int pageNo, int pageSize) {
        Long artistId = requireArtistId();
        Page<ArtistWithdrawalDO> page = new Page<>(Math.max(pageNo, 1), Math.min(Math.max(pageSize, 1), 100));
        IPage<ArtistWithdrawalDO> result = withdrawalMapper.selectPage(page,
                Wrappers.lambdaQuery(ArtistWithdrawalDO.class)
                        .eq(ArtistWithdrawalDO::getArtistId, artistId)
                        .orderByDesc(ArtistWithdrawalDO::getCreateTime));
        return result.convert(this::toResponse);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelWithdrawal(Long withdrawalId) {
        Long artistId = requireArtistId();
        ArtistWithdrawalDO withdrawal = withdrawalMapper.selectOne(Wrappers.lambdaQuery(ArtistWithdrawalDO.class)
                .eq(ArtistWithdrawalDO::getId, withdrawalId)
                .eq(ArtistWithdrawalDO::getArtistId, artistId));
        if (withdrawal == null) {
            throw new ClientException("提现申请不存在");
        }
        if (!Integer.valueOf(0).equals(withdrawal.getStatus())) {
            throw new ClientException("当前提现申请不可取消");
        }
        if (withdrawalMapper.update(null, Wrappers.lambdaUpdate(ArtistWithdrawalDO.class)
                .eq(ArtistWithdrawalDO::getId, withdrawalId)
                .eq(ArtistWithdrawalDO::getStatus, 0)
                .set(ArtistWithdrawalDO::getStatus, 4)) <= 0) {
            throw new ClientException("提现申请状态已变更");
        }
        if (accountMapper.release(artistId, withdrawal.getAmount()) <= 0) {
            throw new ServiceException("释放提现冻结余额失败");
        }
        insertLedger(artistId, "WITHDRAW_RELEASE", withdrawal.getRequestNo(), LEDGER_CREDIT,
                withdrawal.getAmount(), getOrCreateAccount(artistId).getAvailableAmount(), "取消提现释放冻结");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeWithdrawal(Long withdrawalId, String externalNo) {
        requireSuperAdmin();
        ArtistWithdrawalDO withdrawal = withdrawalMapper.selectById(withdrawalId);
        if (withdrawal == null || (!Integer.valueOf(0).equals(withdrawal.getStatus())
                && !Integer.valueOf(1).equals(withdrawal.getStatus()))) {
            throw new ClientException("提现申请不存在或不可完成");
        }
        if (withdrawalMapper.update(null, Wrappers.lambdaUpdate(ArtistWithdrawalDO.class)
                .eq(ArtistWithdrawalDO::getId, withdrawalId)
                .in(ArtistWithdrawalDO::getStatus, List.of(0, 1))
                .set(ArtistWithdrawalDO::getStatus, 2)
                .set(ArtistWithdrawalDO::getExternalNo, externalNo)
                .set(ArtistWithdrawalDO::getCompletedTime, new Date())) <= 0) {
            throw new ClientException("提现申请状态已变更");
        }
        if (accountMapper.completeWithdrawal(withdrawal.getArtistId(), withdrawal.getAmount()) <= 0) {
            throw new ServiceException("扣除提现冻结余额失败");
        }
        insertLedger(withdrawal.getArtistId(), "WITHDRAW_PAID", withdrawal.getRequestNo(), LEDGER_DEBIT,
                withdrawal.getAmount(), getOrCreateAccount(withdrawal.getArtistId()).getAvailableAmount(), "提现完成");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void creditCommission(Long artistId, Long commissionRecordId, BigDecimal amount) {
        if (artistId == null || commissionRecordId == null || amount == null || amount.signum() <= 0) {
            throw new ServiceException("佣金入账参数不合法");
        }
        RLock lock = redissonClient.getLock("livestart:artist:wallet:credit:" + artistId);
        lock.lock();
        try {
            String bizNo = String.valueOf(commissionRecordId);
            ArtistWalletLedgerDO existing = ledgerMapper.selectOne(Wrappers.lambdaQuery(ArtistWalletLedgerDO.class)
                    .eq(ArtistWalletLedgerDO::getArtistId, artistId)
                    .eq(ArtistWalletLedgerDO::getBizType, "COMMISSION")
                    .eq(ArtistWalletLedgerDO::getBizNo, bizNo));
            if (existing != null) {
                return;
            }
            getOrCreateAccount(artistId);
            if (accountMapper.credit(artistId, amount) <= 0) {
                throw new ServiceException("佣金入账失败");
            }
            ArtistWalletAccountDO account = getOrCreateAccount(artistId);
            insertLedger(artistId, "COMMISSION", bizNo, LEDGER_CREDIT, amount,
                    account.getAvailableAmount(), "推广佣金到账");
        } finally {
            lock.unlock();
        }
    }

    private ArtistWalletAccountDO getOrCreateAccount(Long artistId) {
        ArtistWalletAccountDO account = accountMapper.selectById(artistId);
        if (account != null) {
            return account;
        }
        ArtistWalletAccountDO created = ArtistWalletAccountDO.builder()
                .artistId(artistId)
                .availableAmount(BigDecimal.ZERO)
                .frozenAmount(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .version(0)
                .build();
        try {
            accountMapper.insert(created);
            return created;
        } catch (Exception ignored) {
            ArtistWalletAccountDO existing = accountMapper.selectById(artistId);
            if (existing == null) {
                throw new ServiceException("创建艺人钱包失败");
            }
            return existing;
        }
    }

    private void insertLedger(Long artistId, String bizType, String bizNo, int direction,
                              BigDecimal amount, BigDecimal balanceAfter, String remark) {
        ArtistWalletLedgerDO ledger = ArtistWalletLedgerDO.builder()
                .artistId(artistId)
                .bizType(bizType)
                .bizNo(bizNo)
                .direction(direction)
                .amount(amount)
                .balanceAfter(balanceAfter)
                .remark(remark)
                .build();
        if (ledgerMapper.insert(ledger) <= 0) {
            throw new ServiceException("钱包流水写入失败");
        }
    }

    private ArtistWithdrawalRespDTO toResponse(ArtistWithdrawalDO source) {
        ArtistWithdrawalRespDTO target = new ArtistWithdrawalRespDTO();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new ClientException("提现金额必须大于0");
        }
        if (amount.scale() > 2) {
            throw new ClientException("提现金额最多保留两位小数");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void ensureSameWithdrawal(ArtistWithdrawalDO withdrawal, Long artistId, BigDecimal amount,
                                      String accountType, String accountNo, String accountName) {
        if (!artistId.equals(withdrawal.getArtistId())) {
            throw new ClientException("提现请求号已被其他艺人使用");
        }
        if (withdrawal.getAmount() == null || withdrawal.getAmount().compareTo(amount) != 0
                || !Objects.equals(withdrawal.getAccountType(), accountType)
                || !Objects.equals(withdrawal.getAccountNo(), accountNo)
                || !Objects.equals(withdrawal.getAccountName(), accountName)) {
            throw new ClientException("提现请求号对应的申请内容不一致");
        }
    }

    private Long requireArtistId() {
        if (!Integer.valueOf(ARTIST_USER_TYPE).equals(UserContext.getUserType())) {
            throw new ClientException("当前账号不是艺人账号");
        }
        String userId = UserContext.getUserId();
        try {
            return Long.valueOf(userId);
        } catch (Exception ex) {
            throw new ClientException("当前用户身份无效");
        }
    }

    private void requireSuperAdmin() {
        if (!Integer.valueOf(SUPER_ADMIN_USER_TYPE).equals(UserContext.getUserType())) {
            throw new ClientException("只有超级管理员可以完成提现");
        }
    }
}
