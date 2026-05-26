package com.mongxin.livestart.distribution.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.distribution.common.biz.user.UserContext;
import com.mongxin.livestart.distribution.common.constant.DistributionRedisConstant;
import com.mongxin.livestart.distribution.common.enums.CommissionStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.ArtistCommissionRecordDO;
import com.mongxin.livestart.distribution.dao.entity.InviteCodeDO;
import com.mongxin.livestart.distribution.dao.entity.InviteRelationDO;
import com.mongxin.livestart.distribution.dao.entity.OrderDO;
import com.mongxin.livestart.distribution.dao.mapper.ArtistCommissionRecordMapper;
import com.mongxin.livestart.distribution.dao.mapper.InviteCodeMapper;
import com.mongxin.livestart.distribution.dao.mapper.InviteRelationMapper;
import com.mongxin.livestart.distribution.dao.mapper.OrderMapper;
import com.mongxin.livestart.distribution.dto.resp.ArtistCommissionRespDTO;
import com.mongxin.livestart.distribution.dto.resp.InviteCodeRespDTO;
import com.mongxin.livestart.distribution.mq.event.CommissionSettleEvent;
import com.mongxin.livestart.distribution.mq.event.OrderPaySuccessEvent;
import com.mongxin.livestart.distribution.mq.producer.CommissionSettleProducer;
import com.mongxin.livestart.distribution.service.ArtistCommissionService;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 艺人推广个税代扣及提成到账结算服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistCommissionServiceImpl extends ServiceImpl<ArtistCommissionRecordMapper, ArtistCommissionRecordDO> implements ArtistCommissionService {

    private final InviteCodeMapper inviteCodeMapper;
    private final InviteRelationMapper inviteRelationMapper;
    private final OrderMapper orderMapper;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final CommissionSettleProducer commissionSettleProducer;

    /** 固定艺人宣发票房分成比率 (10%) */
    private static final BigDecimal DEFAULT_COMMISSION_RATE = new BigDecimal("0.10");
    /** 劳务报酬平台简易代扣个税比率 (20%) */
    private static final BigDecimal DEFAULT_TAX_RATE = new BigDecimal("0.20");
    /** 退票保障提成结算延时：15天。测试可以用较短时间，例如 15分钟 */
    private static final long SETTLE_DELAY_MS = 15L * 24 * 3600 * 1000;

    @Override
    public InviteCodeRespDTO getOrCreateArtistPromoCode() {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new ClientException("艺人用户未登录");
        }
        Long artistId = Long.parseLong(userIdStr);

        // 1. 查询是否已有推广宣发码
        InviteCodeDO inviteCodeDO = inviteCodeMapper.selectOne(
                Wrappers.lambdaQuery(InviteCodeDO.class).eq(InviteCodeDO::getUserId, artistId)
        );

        if (inviteCodeDO == null) {
            String lockKey = String.format(DistributionRedisConstant.ARTIST_CODE_GEN_LOCK, artistId);
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock();
            try {
                // 双检
                inviteCodeDO = inviteCodeMapper.selectOne(
                        Wrappers.lambdaQuery(InviteCodeDO.class).eq(InviteCodeDO::getUserId, artistId)
                );
                if (inviteCodeDO == null) {
                    inviteCodeDO = doGeneratePromoCode(artistId);
                }
            } finally {
                lock.unlock();
            }
        }

        // 2. 统计累计票房和税后已到手佣金、待结算佣金
        Long inviteeCount = inviteRelationMapper.selectCount(
                Wrappers.lambdaQuery(InviteRelationDO.class).eq(InviteRelationDO::getInviterUserId, artistId)
        );

        List<ArtistCommissionRecordDO> records = baseMapper.selectList(
                Wrappers.lambdaQuery(ArtistCommissionRecordDO.class).eq(ArtistCommissionRecordDO::getArtistId, artistId)
        );

        BigDecimal settledAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        for (ArtistCommissionRecordDO record : records) {
            if (CommissionStatusEnum.SETTLED.getCode() == record.getStatus()) {
                settledAmount = settledAmount.add(record.getActualAmount()); // 到账已代扣税后的实际所得
            } else if (CommissionStatusEnum.PENDING.getCode() == record.getStatus()) {
                pendingAmount = pendingAmount.add(record.getActualAmount()); // 税后在途的待结算金额
            }
        }

        InviteCodeRespDTO respDTO = new InviteCodeRespDTO();
        respDTO.setUserId(artistId);
        respDTO.setInviteCode(inviteCodeDO.getInviteCode());
        respDTO.setInviteeCount(inviteeCount.intValue());
        respDTO.setSettledCommission(settledAmount);
        respDTO.setPendingCommission(pendingAmount);

        return respDTO;
    }

    private InviteCodeDO doGeneratePromoCode(Long artistId) {
        String code = null;
        boolean unique = false;
        int maxRetries = 10;

        while (!unique && maxRetries > 0) {
            code = RandomUtil.randomString(6).toUpperCase();
            String redisKey = String.format(DistributionRedisConstant.ARTIST_PROMO_CODE_KEY, code);
            Boolean hasKey = stringRedisTemplate.hasKey(redisKey);
            if (Boolean.FALSE.equals(hasKey)) {
                // 数据库校验
                Long count = inviteCodeMapper.selectCount(
                        Wrappers.lambdaQuery(InviteCodeDO.class).eq(InviteCodeDO::getInviteCode, code)
                );
                if (count == 0) {
                    unique = true;
                }
            }
            maxRetries--;
        }

        if (!unique) {
            throw new ServiceException("生成推广专属码失败，重试冲突");
        }

        InviteCodeDO codeDO = InviteCodeDO.builder()
                .userId(artistId)
                .inviteCode(code)
                .build();
        inviteCodeMapper.insert(codeDO);

        // Redis 缓存映射
        String redisKey = String.format(DistributionRedisConstant.ARTIST_PROMO_CODE_KEY, code);
        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(artistId));

        return codeDO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processOrderPaySuccess(OrderPaySuccessEvent event) {
        Long inviteeUserId = event.getUserId();
        String orderNo = event.getOrderNo();

        // 1. 查询购票歌迷是否有绑定的推广渠道关系
        InviteRelationDO relation = inviteRelationMapper.selectOne(
                Wrappers.lambdaQuery(InviteRelationDO.class).eq(InviteRelationDO::getInviteeUserId, inviteeUserId)
        );
        if (relation == null) {
            log.info("[分销分成] 购票歌迷没有推荐人艺人，忽略，userId={}", inviteeUserId);
            return;
        }
        Long artistId = relation.getInviterUserId();

        // 2. 利用追加的分表路由查购票实付票房款
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, orderNo)
                .eq(OrderDO::getUserId, inviteeUserId);
        OrderDO order = orderMapper.selectOne(queryWrapper);

        if (order == null) {
            log.warn("[分销分成] 未查询到歌迷购票分表的关联订单金额，orderNo={}", orderNo);
            return;
        }

        // 3. 税务与票房分成代扣计算
        BigDecimal ticketAmount = order.getTotalAmount();
        BigDecimal commissionAmount = ticketAmount.multiply(DEFAULT_COMMISSION_RATE); // 税前提成
        BigDecimal taxAmount = commissionAmount.multiply(DEFAULT_TAX_RATE); // 代扣税费 (20%)
        BigDecimal actualAmount = commissionAmount.subtract(taxAmount); // 税后实得 (80%)

        ArtistCommissionRecordDO record = ArtistCommissionRecordDO.builder()
                .artistId(artistId)
                .artistPromoCode(relation.getInviteCode())
                .orderNo(orderNo)
                .ticketAmount(ticketAmount)
                .commissionRate(DEFAULT_COMMISSION_RATE)
                .commissionAmount(commissionAmount)
                .taxRate(DEFAULT_TAX_RATE)
                .taxAmount(taxAmount)
                .actualAmount(actualAmount)
                .status(CommissionStatusEnum.PENDING.getCode())
                .build();

        baseMapper.insert(record);
        log.info("[分销分成] 歌迷购票成功出票！已为艺人创建待到账分成明细 (已税后核算)，artistId={}，歌迷支付={}，分成额(税前)={}，代扣个税={}，艺人税后实得={}",
                artistId, ticketAmount, commissionAmount, taxAmount, actualAmount);

        // 4. 发送 15 天后延迟正式到账入账结算消息，保障退票周期
        CommissionSettleEvent settleEvent = CommissionSettleEvent.builder()
                .commissionRecordId(record.getId())
                .orderNo(orderNo)
                .action(1) // 1-正式到账
                .build();

        try {
            commissionSettleProducer.sendDelayMessage(settleEvent, SETTLE_DELAY_MS);
            log.info("[分销分成] 已成功向 MQ 投递个税延迟结算通知，将于 15 天后到账，recordId={}", record.getId());
        } catch (Exception e) {
            log.error("[分销分成] 发送延迟结算消息到 MQ 异常，recordId={}", record.getId(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void settleCommission(CommissionSettleEvent event) {
        Long recordId = event.getCommissionRecordId();
        ArtistCommissionRecordDO record = baseMapper.selectById(recordId);
        if (record == null) {
            log.warn("[个税结算] 票房分成记录不存在，recordId={}", recordId);
            return;
        }

        if (record.getStatus() == null || CommissionStatusEnum.PENDING.getCode() != record.getStatus()) {
            log.info("[个税结算] 佣金状态已发生变更，过滤该结算，recordId={}，status={}", recordId, record.getStatus());
            return;
        }

        if (event.getAction() == 1) {
            // 正式入账
            int affected = baseMapper.update(null, Wrappers.lambdaUpdate(ArtistCommissionRecordDO.class)
                    .eq(ArtistCommissionRecordDO::getId, recordId)
                    .eq(ArtistCommissionRecordDO::getStatus, CommissionStatusEnum.PENDING.getCode())
                    .set(ArtistCommissionRecordDO::getStatus, CommissionStatusEnum.SETTLED.getCode())
                    .set(ArtistCommissionRecordDO::getSettleTime, new Date())
            );
            if (affected > 0) {
                log.info("[个税结算] 提成正式结算入账成功，已代扣个税打入艺人余额，artistId={}，票房={}, 艺人税后净得={}",
                        record.getArtistId(), record.getTicketAmount(), record.getActualAmount());
            }
        } else if (event.getAction() == 2) {
            // 退票取消分成
            int affected = baseMapper.update(null, Wrappers.lambdaUpdate(ArtistCommissionRecordDO.class)
                    .eq(ArtistCommissionRecordDO::getId, recordId)
                    .eq(ArtistCommissionRecordDO::getStatus, CommissionStatusEnum.PENDING.getCode())
                    .set(ArtistCommissionRecordDO::getStatus, CommissionStatusEnum.CANCELLED.getCode())
            );
            if (affected > 0) {
                log.info("[个税结算] 歌迷退款退票，已取消分成及个税，recordId={}", recordId);
            }
        }
    }

    @Override
    public IPage<ArtistCommissionRespDTO> pageQueryArtistCommissions(int pageNo, int pageSize, Integer status) {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new ClientException("艺人用户未登录");
        }
        Long artistId = Long.parseLong(userIdStr);

        Page<ArtistCommissionRecordDO> requestPage = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<ArtistCommissionRecordDO> query = Wrappers.lambdaQuery(ArtistCommissionRecordDO.class)
                .eq(ArtistCommissionRecordDO::getArtistId, artistId)
                .eq(status != null, ArtistCommissionRecordDO::getStatus, status)
                .orderByDesc(ArtistCommissionRecordDO::getCreateTime);

        IPage<ArtistCommissionRecordDO> pageResult = baseMapper.selectPage(requestPage, query);
        return pageResult.convert(item -> {
            ArtistCommissionRespDTO dto = new ArtistCommissionRespDTO();
            dto.setId(item.getId());
            dto.setArtistId(item.getArtistId());
            dto.setArtistPromoCode(item.getArtistPromoCode());
            dto.setOrderNo(item.getOrderNo());
            dto.setTicketAmount(item.getTicketAmount());
            dto.setCommissionRate(item.getCommissionRate());
            dto.setCommissionAmount(item.getCommissionAmount());
            dto.setTaxRate(item.getTaxRate());
            dto.setTaxAmount(item.getTaxAmount());
            dto.setActualAmount(item.getActualAmount());
            dto.setStatus(item.getStatus());
            dto.setStatusDesc(CommissionStatusEnum.values()[item.getStatus()].getDesc());
            dto.setSettleTime(item.getSettleTime());
            dto.setCreateTime(item.getCreateTime());
            return dto;
        });
    }
}
