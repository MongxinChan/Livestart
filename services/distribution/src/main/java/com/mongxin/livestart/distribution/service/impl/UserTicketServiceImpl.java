package com.mongxin.livestart.distribution.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.mongxin.livestart.distribution.common.biz.user.UserContext;
import com.mongxin.livestart.distribution.common.constant.DistributionRedisConstant;
import com.mongxin.livestart.distribution.common.enums.TicketStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import com.mongxin.livestart.distribution.dao.entity.UserTicketDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.dao.mapper.UserTicketMapper;
import com.mongxin.livestart.distribution.dto.req.TicketGrabReqDTO;
import com.mongxin.livestart.distribution.dto.resp.UserTicketRespDTO;
import com.mongxin.livestart.distribution.feign.MerchantAdminRemoteService;
import com.mongxin.livestart.distribution.feign.dto.MerchantVenueRespDTO;
import com.mongxin.livestart.distribution.service.UserTicketService;
import com.mongxin.livestart.distribution.toolkit.StockDecrementReturnCombinedUtil;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserTicketServiceImpl extends ServiceImpl<UserTicketMapper, UserTicketDO> implements UserTicketService {

    private static final String STOCK_DECREMENT_LUA_PATH = "lua/stock_decrement.lua";
    private static final String STOCK_ROLLBACK_LUA_PATH = "lua/stock_rollback.lua";
    private static final long LIMIT_KEY_EXPIRE_SECONDS = 7 * 24 * 3600L;

    private final TicketSkuMapper ticketSkuMapper;
    private final EventMapper eventMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final MerchantAdminRemoteService merchantAdminRemoteService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grabTicket(TicketGrabReqDTO requestParam) {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new ClientException("歌迷未登录，无法抢票");
        }

        Long userId = Long.parseLong(userIdStr);
        Long ticketSkuId = requestParam.getTicketSkuId();
        TicketSkuDO sku = ticketSkuMapper.selectById(ticketSkuId);
        if (sku == null) {
            throw new ClientException("该门票票档不存在");
        }
        if (sku.getRemainingStock() <= 0) {
            throw new ClientException("该门票已被秒杀光啦");
        }

        DefaultRedisScript<Long> decrementScript = loadLongRedisScript(STOCK_DECREMENT_LUA_PATH);
        String stockKey = String.format(DistributionRedisConstant.TICKET_STOCK_KEY, ticketSkuId);
        String limitKey = String.format(DistributionRedisConstant.TICKET_USER_LIMIT_KEY, ticketSkuId, userId);
        int maxLimit = sku.getLimitNum() != null ? sku.getLimitNum() : 2;

        Long luaResult = stringRedisTemplate.execute(
                decrementScript,
                List.of(stockKey, limitKey),
                "1",
                String.valueOf(maxLimit),
                String.valueOf(LIMIT_KEY_EXPIRE_SECONDS)
        );
        if (luaResult == null) {
            throw new ServiceException("抢票火爆，请稍后再试");
        }

        long errorCode = StockDecrementReturnCombinedUtil.extractErrorCode(luaResult);
        if (errorCode == 1L) {
            throw new ClientException("门票已被抢光啦");
        }
        if (errorCode == 2L) {
            throw new ClientException(String.format("您已达到该演出票档的秒杀限购上限 (限购 %d 张)", maxLimit));
        }

        try {
            Integer version = sku.getVersion() != null ? sku.getVersion() : 0;
            int decremented = ticketSkuMapper.decrementStock(ticketSkuId, 1, version);
            if (!SqlHelper.retBool(decremented)) {
                throw new ServiceException("秒杀库存不足，冲突退回，请重试");
            }

            String uniqueCheckCode = UUID.fastUUID().toString(true).toUpperCase();
            UserTicketDO userTicket = UserTicketDO.builder()
                    .userId(userId)
                    .ticketSkuId(ticketSkuId)
                    .eventId(sku.getEventId())
                    .status(TicketStatusEnum.UNUSED.getCode())
                    .checkCode(uniqueCheckCode)
                    .artistPromoCode(requestParam.getArtistPromoCode())
                    .build();
            save(userTicket);

            log.info("[门票秒杀] 抢票成功，userId={}, eventId={}, checkCode={}, promoCode={}",
                    userId, sku.getEventId(), uniqueCheckCode, requestParam.getArtistPromoCode());
        } catch (Exception ex) {
            rollbackPreDeductStock(stockKey, limitKey, 1);
            if (ex instanceof ClientException || ex instanceof ServiceException) {
                throw ex;
            }
            throw new ServiceException("系统繁忙，秒杀失败，请稍后重试");
        }
    }

    @Override
    public IPage<UserTicketRespDTO> pageQueryUserTickets(int pageNo, int pageSize, Integer status) {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new ClientException("歌迷未登录，无法检索");
        }
        Long userId = Long.parseLong(userIdStr);

        Page<UserTicketDO> requestPage = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<UserTicketDO> queryWrapper = Wrappers.lambdaQuery(UserTicketDO.class)
                .eq(UserTicketDO::getUserId, userId)
                .eq(status != null, UserTicketDO::getStatus, status)
                .orderByDesc(UserTicketDO::getCreateTime);

        IPage<UserTicketDO> pageResult = page(requestPage, queryWrapper);
        if (CollUtil.isEmpty(pageResult.getRecords())) {
            return new Page<>(pageNo, pageSize);
        }

        Set<Long> skuIds = pageResult.getRecords().stream()
                .map(UserTicketDO::getTicketSkuId)
                .collect(Collectors.toSet());
        Set<Long> eventIds = pageResult.getRecords().stream()
                .map(UserTicketDO::getEventId)
                .collect(Collectors.toSet());

        Map<Long, TicketSkuDO> skuMap = ticketSkuMapper.selectBatchIds(skuIds).stream()
                .collect(Collectors.toMap(TicketSkuDO::getId, item -> item));
        Map<Long, EventDO> eventMap = eventMapper.selectBatchIds(eventIds).stream()
                .collect(Collectors.toMap(EventDO::getId, item -> item));
        Map<Long, MerchantVenueRespDTO> venueCache = new HashMap<>();

        return pageResult.convert(item -> {
            UserTicketRespDTO dto = new UserTicketRespDTO();
            dto.setId(item.getId());
            dto.setEventId(item.getEventId());
            dto.setTicketSkuId(item.getTicketSkuId());
            dto.setStatus(item.getStatus());
            dto.setStatusDesc(resolveTicketStatusDesc(item.getStatus()));
            dto.setCheckCode(item.getCheckCode());
            dto.setArtistPromoCode(item.getArtistPromoCode());
            dto.setCreateTime(item.getCreateTime());

            TicketSkuDO sku = skuMap.get(item.getTicketSkuId());
            if (sku != null) {
                dto.setTicketSkuTitle(sku.getTitle());
                dto.setPrice(sku.getSellingPrice());
            }

            EventDO event = eventMap.get(item.getEventId());
            if (event != null) {
                dto.setEventTitle(event.getTitle());
                dto.setEventTime(event.getEventTime());
                if (event.getVenueId() != null) {
                    MerchantVenueRespDTO venueDTO = venueCache.computeIfAbsent(event.getVenueId(), id -> {
                        try {
                            var venueResult = merchantAdminRemoteService.getVenue(id);
                            if (venueResult.isSuccess() && venueResult.getData() != null) {
                                return venueResult.getData();
                            }
                        } catch (Exception e) {
                            log.error("[distribution] failed to query venue detail, venueId={}", id, e);
                        }
                        return null;
                    });
                    if (venueDTO != null) {
                        dto.setAddress(venueDTO.getAddress());
                    }
                }
            }
            return dto;
        });
    }

    private void rollbackPreDeductStock(String stockKey, String limitKey, int count) {
        DefaultRedisScript<Long> rollbackScript = loadLongRedisScript(STOCK_ROLLBACK_LUA_PATH);
        try {
            stringRedisTemplate.execute(
                    rollbackScript,
                    List.of(stockKey, limitKey),
                    String.valueOf(count),
                    String.valueOf(count)
            );
        } catch (Exception redisEx) {
            log.error("[门票秒杀] Redis 预扣回滚失败，stockKey={}, limitKey={}, count={}",
                    stockKey, limitKey, count, redisEx);
        }
    }

    private DefaultRedisScript<Long> loadLongRedisScript(String classpath) {
        return Singleton.get(classpath, () -> {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource(classpath)));
            script.setResultType(Long.class);
            return script;
        });
    }

    private String resolveTicketStatusDesc(Integer statusCode) {
        if (statusCode == null) {
            return "未知状态";
        }
        for (TicketStatusEnum value : TicketStatusEnum.values()) {
            if (value.getCode() == statusCode) {
                return value.getDesc();
            }
        }
        return "未知状态";
    }
}
