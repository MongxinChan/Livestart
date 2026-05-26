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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 歌迷电子门票秒杀抢购与检索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTicketServiceImpl extends ServiceImpl<UserTicketMapper, UserTicketDO> implements UserTicketService {

    private final TicketSkuMapper ticketSkuMapper;
    private final EventMapper eventMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String STOCK_DECREMENT_LUA_PATH = "lua/stock_decrement.lua";
    /** Redis 记录用户秒杀限购的过期时间（7天，供核销与追溯） */
    private static final long LIMIT_KEY_EXPIRE_SECONDS = 7 * 24 * 3600L;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grabTicket(TicketGrabReqDTO requestParam) {
        String userIdStr = UserContext.getUserId();
        if (StrUtil.isBlank(userIdStr)) {
            throw new ClientException("歌迷未登录，无法抢票");
        }
        Long userId = Long.parseLong(userIdStr);
        Long ticketSkuId = requestParam.getTicketSkuId();

        // 1. 查询门票票档是否存在
        TicketSkuDO sku = ticketSkuMapper.selectById(ticketSkuId);
        if (sku == null) {
            throw new ClientException("该门票票档不存在");
        }
        if (sku.getRemainingStock() <= 0) {
            throw new ClientException("该门票已被秒杀光啦");
        }

        // 2. 运行 Lua 脚本原子预扣减 Redis 库存并进行个人限购数校验
        DefaultRedisScript<Long> luaScript = Singleton.get(STOCK_DECREMENT_LUA_PATH, () -> {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>();
            script.setScriptSource(new ResourceScriptSource(new ClassPathResource(STOCK_DECREMENT_LUA_PATH)));
            script.setResultType(Long.class);
            return script;
        });

        String stockKey = String.format(DistributionRedisConstant.TICKET_STOCK_KEY, ticketSkuId);
        String limitKey = String.format(DistributionRedisConstant.TICKET_USER_LIMIT_KEY, ticketSkuId, userId);
        int maxLimit = sku.getLimitNum() != null ? sku.getLimitNum() : 2;

        Long luaResult = stringRedisTemplate.execute(
                luaScript,
                List.of(stockKey, limitKey),
                "1",
                String.valueOf(maxLimit),
                String.valueOf(LIMIT_KEY_EXPIRE_SECONDS)
        );

        if (luaResult == null) {
            throw new ServiceException("抢票火爆，请稍后再试");
        }

        // 位移解包提取高位错误码
        long errorCode = StockDecrementReturnCombinedUtil.extractErrorCode(luaResult);
        if (errorCode == 1) {
            throw new ClientException("门票已被抢光啦");
        } else if (errorCode == 2) {
            throw new ClientException(String.format("您已达到该演出票档的秒杀限购上限 (限购 %d 张)", maxLimit));
        }

        // 3. 编程式/声明式事务：DB 端乐观锁扣减物理库存并写入领券分表
        try {
            // DB 级扣减物理库存 (版本号乐观锁控制)
            int decremented = ticketSkuMapper.update(null, Wrappers.lambdaUpdate(TicketSkuDO.class)
                    .eq(TicketSkuDO::getId, ticketSkuId)
                    .gt(TicketSkuDO::getRemainingStock, 0)
                    .setSql("remaining_stock = remaining_stock - 1")
            );

            if (!SqlHelper.retBool(decremented)) {
                throw new ServiceException("秒杀库存不足，冲突退回，请重试");
            }

            // 写入已抢领的门票分表 (按 userId 分 16 表存储在 ds_order 库中)
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
            log.info("[门票秒杀] 抢特价票成功！userId={}，eventId={}，checkCode={}，PromoCode={}",
                    userId, sku.getEventId(), uniqueCheckCode, requestParam.getArtistPromoCode());

        } catch (Exception ex) {
            // 补偿回退：抢票失败将 Redis 预扣库存恢复
            try {
                stringRedisTemplate.opsForValue().increment(stockKey);
                stringRedisTemplate.opsForValue().decrement(limitKey);
            } catch (Exception redisEx) {
                log.error("[门票秒杀] 抢票失败库存回滚补偿 Redis 异常，skuId={}，userId={}", ticketSkuId, userId, redisEx);
            }
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

        // 利用 ShardingSphere 精准定位到 ds_order 库下的对应分表执行分页
        IPage<UserTicketDO> pageResult = page(requestPage, queryWrapper);
        if (CollUtil.isEmpty(pageResult.getRecords())) {
            return new Page<>(pageNo, pageSize);
        }

        // 抽取所有的 ticketSkuId 和 eventId，在 Java 内存中批量查询拼装，彻底免除跨分库 Join 的毁灭级大坑
        Set<Long> skuIds = pageResult.getRecords().stream()
                .map(UserTicketDO::getTicketSkuId)
                .collect(Collectors.toSet());
        Set<Long> eventIds = pageResult.getRecords().stream()
                .map(UserTicketDO::getEventId)
                .collect(Collectors.toSet());

        List<TicketSkuDO> skus = ticketSkuMapper.selectBatchIds(skuIds);
        Map<Long, TicketSkuDO> skuMap = skus.stream()
                .collect(Collectors.toMap(TicketSkuDO::getId, item -> item));

        List<EventDO> events = eventMapper.selectBatchIds(eventIds);
        Map<Long, EventDO> eventMap = events.stream()
                .collect(Collectors.toMap(EventDO::getId, item -> item));

        return pageResult.convert(item -> {
            UserTicketRespDTO dto = new UserTicketRespDTO();
            dto.setId(item.getId());
            dto.setEventId(item.getEventId());
            dto.setTicketSkuId(item.getTicketSkuId());
            dto.setStatus(item.getStatus());
            dto.setStatusDesc(TicketStatusEnum.values()[item.getStatus()].getDesc());
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
                dto.setAddress(event.getAddress());
            }

            return dto;
        });
    }
}
