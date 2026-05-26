package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventMapper;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import com.mongxin.livestart.merchant.admin.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 演出服务实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {

    private final EventConfigService eventConfigService;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String EVENT_CACHE_KEY_PREFIX = "livestart:event:detail:%d";

    /** 演出状态：0-下架 1-预售 2-在售 3-售罄 */
    private static final int STATUS_OFF_SHELF = 0;
    private static final int STATUS_PRESALE = 1;
    private static final int STATUS_ON_SALE = 2;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEvent(EventDO requestParam) {
        if (requestParam == null || requestParam.getVenueId() == null || requestParam.getStartTime() == null) {
            throw new ClientException("创建演出核心属性(startTime/venueId)不能为空");
        }

        // 保存演出主记录
        save(requestParam);

        // 级联初始化默认演出配置
        EventConfigDO defaultConfig = new EventConfigDO();
        defaultConfig.setEventId(requestParam.getId());
        defaultConfig.setSelectionMode(0);
        defaultConfig.setIsVerifyRequired(0);
        defaultConfig.setMaxTicketsPerUser(4);
        defaultConfig.setRefundPolicyType(1);
        defaultConfig.setTier1FreeRefundHours(48);
        defaultConfig.setIsWaitingAllowed(0);
        defaultConfig.setIsTransferable(0);
        eventConfigService.save(defaultConfig);

        // 缓存预热
        warmUpEventCache(requestParam, defaultConfig);
    }

    @Override
    public List<EventDO> listAllEvents() {
        return list();
    }

    @Override
    public IPage<EventDO> pageQueryEvents(Page<EventDO> page, Integer status) {
        LambdaQueryWrapper<EventDO> queryWrapper = Wrappers.lambdaQuery(EventDO.class)
                .eq(status != null, EventDO::getStatus, status)
                .orderByDesc(EventDO::getId);
        return baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public EventDO getEventById(Long id) {
        return getById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateEvent(EventDO requestParam) {
        updateById(requestParam);

        // 重新捞取最新全量数据刷新缓存
        EventDO latestEvent = getById(requestParam.getId());
        EventConfigDO latestConfig = eventConfigService.getByEventId(requestParam.getId());
        if (latestEvent != null && latestConfig != null) {
            warmUpEventCache(latestEvent, latestConfig);
            log.info("演出修改完成 & 缓存已同步刷新 | eventId={}", requestParam.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteEvent(Long id) {
        LambdaQueryWrapper<EventConfigDO> configQuery = Wrappers.lambdaQuery(EventConfigDO.class)
                .eq(EventConfigDO::getEventId, id);
        eventConfigService.remove(configQuery);
        removeById(id);

        // 同步清除 Redis 缓存
        try {
            stringRedisTemplate.delete(String.format(EVENT_CACHE_KEY_PREFIX, id));
            log.info("演出删除完成 & 缓存已清除 | eventId={}", id);
        } catch (Exception e) {
            log.error("演出缓存清除失败（非阻塞） | eventId={}", id, e);
        }
    }

    @Override
    public void publishEvent(Long id) {
        EventDO event = getById(id);
        if (event == null) {
            throw new ClientException("演出不存在");
        }
        if (ObjectUtil.notEqual(event.getStatus(), STATUS_PRESALE)) {
            throw new ClientException("仅预售状态的演出可以上架开售");
        }

        EventDO update = new EventDO();
        update.setId(id);
        update.setStatus(STATUS_ON_SALE);
        updateById(update);

        // 同步刷新缓存中的 status 字段
        syncCacheField(id, "status", String.valueOf(STATUS_ON_SALE));
        log.info("演出已上架开售 | eventId={}", id);
    }

    @Override
    public void shelveEvent(Long id) {
        EventDO event = getById(id);
        if (event == null) {
            throw new ClientException("演出不存在");
        }
        if (ObjectUtil.notEqual(event.getStatus(), STATUS_ON_SALE)) {
            throw new ClientException("仅在售状态的演出可以下架");
        }

        EventDO update = new EventDO();
        update.setId(id);
        update.setStatus(STATUS_OFF_SHELF);
        updateById(update);

        syncCacheField(id, "status", String.valueOf(STATUS_OFF_SHELF));
        log.info("演出已下架 | eventId={}", id);
    }

    @Override
    public void terminateEvent(Long id) {
        EventDO event = getById(id);
        if (event == null) {
            throw new ClientException("演出不存在");
        }
        if (ObjectUtil.equal(event.getStatus(), STATUS_OFF_SHELF)) {
            throw new ClientException("演出已处于下架状态，无需终止");
        }

        EventDO update = new EventDO();
        update.setId(id);
        update.setStatus(STATUS_OFF_SHELF);
        updateById(update);

        syncCacheField(id, "status", String.valueOf(STATUS_OFF_SHELF));
        log.info("演出已终止售票 | eventId={}", id);
    }

    /**
     * 同步刷新 Redis Hash 中的单个字段（轻量级，学习 terminateCouponTemplate 的简约路线）
     */
    private void syncCacheField(Long eventId, String field, String value) {
        try {
            String cacheKey = String.format(EVENT_CACHE_KEY_PREFIX, eventId);
            stringRedisTemplate.opsForHash().put(cacheKey, field, value);
        } catch (Exception e) {
            log.error("演出缓存字段同步失败（非阻塞） | eventId={} field={}", eventId, field, e);
        }
    }

    /**
     * 演出详情缓存预热/全量刷新（Create 和 Update 共用）
     */
    private void warmUpEventCache(EventDO event, EventConfigDO config) {
        try {
            String eventCacheKey = String.format(EVENT_CACHE_KEY_PREFIX, event.getId());
            Map<String, String> cacheMap = new HashMap<>();
            cacheMap.put("id", String.valueOf(event.getId()));
            cacheMap.put("title", event.getTitle() != null ? event.getTitle() : "");
            cacheMap.put("eventType", String.valueOf(event.getEventType()));
            cacheMap.put("venueId", String.valueOf(event.getVenueId()));
            cacheMap.put("startTime", String.valueOf(event.getStartTime().getTime()));
            cacheMap.put("posterUrl", event.getPosterUrl() != null ? event.getPosterUrl() : "");
            cacheMap.put("status", String.valueOf(event.getStatus()));
            cacheMap.put("maxTicketsPerUser", String.valueOf(config.getMaxTicketsPerUser()));
            cacheMap.put("isVerifyRequired", String.valueOf(config.getIsVerifyRequired()));

            String luaScript = "redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV - 1)) " +
                               "redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])";

            List<String> args = new ArrayList<>();
            cacheMap.forEach((k, v) -> {
                args.add(k);
                args.add(v);
            });
            long expireTimeSec = (event.getStartTime().getTime() + 24 * 60 * 60 * 1000) / 1000;
            args.add(String.valueOf(expireTimeSec));

            stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                List.of(eventCacheKey),
                args.toArray()
            );
            log.info("演出缓存预热就绪 | eventId={} | expireAt={}", event.getId(), expireTimeSec);
        } catch (Exception e) {
            log.error("演出缓存预热失败（非阻塞） | eventId={}", event.getId(), e);
        }
    }
}
