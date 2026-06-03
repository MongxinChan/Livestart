package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.merchant.admin.common.constant.MerchantAdminRedisConstant;
import com.mongxin.livestart.merchant.admin.common.enums.EventStatusEnum;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventMapper;
import com.mongxin.livestart.merchant.admin.dto.req.EventPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import com.mongxin.livestart.merchant.admin.service.EventService;
import com.mongxin.livestart.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mongxin.livestart.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_EVENT_KEY;

/**
 * 演出服务实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {

    private final EventConfigService eventConfigService;
    private final StringRedisTemplate stringRedisTemplate;
    private final MerchantAdminChainContext merchantAdminChainContext;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 启动时自动用 JDBC 校验并生成多对多关联单表 t_event_performer，规避修改 ShardingSphere 广播表
     */
    @PostConstruct
    public void initEventPerformerTable() {
        try {
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `t_event_performer` (\n" +
                    "  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                    "  `event_id` bigint NOT NULL COMMENT '演出ID',\n" +
                    "  `performer_id` bigint NOT NULL COMMENT '艺人ID',\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  UNIQUE KEY `idx_event_performer` (`event_id`,`performer_id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出艺人关联表';");
            log.info("[merchant-admin] 成功初始化/校验 t_event_performer 演出-艺人关系单表！");
        } catch (Exception e) {
            log.error("[merchant-admin] 自动生成关系表 t_event_performer 失败", e);
        }
    }

    @LogRecord(
            success = """
                    创建演出：{{#requestParam.title}}，\
                    演出类型：{{#requestParam.eventType == 0 ? 'Livehouse(站票)' : '演唱会(选座)' }}，\
                    关联场馆ID：{{#requestParam.venueId}}，\
                    演出时间：{{#requestParam.startTime}};
                    """,
            type = "Event",
            bizNo = "{{#bizNo}}",
            extra = "{{#requestParam.toString()}}"
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEvent(EventSaveReqDTO requestParam) {
        // 通过责任链验证请求参数是否正确
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_EVENT_KEY.name(), requestParam);

        // 保存演出主记录
        EventDO eventDO = BeanUtil.toBean(requestParam, EventDO.class);
        eventDO.setStatus(EventStatusEnum.PRESALE.getStatus());
        save(eventDO);

        // 级联持久化艺人关联
        if (requestParam.getPerformerId() != null) {
            jdbcTemplate.update("INSERT IGNORE INTO t_event_performer (event_id, performer_id) VALUES (?, ?)",
                    eventDO.getId(), requestParam.getPerformerId());
        }

        // 级联初始化默认演出配置
        EventConfigDO defaultConfig = new EventConfigDO();
        defaultConfig.setEventId(eventDO.getId());
        defaultConfig.setSelectionMode(0);
        defaultConfig.setIsVerifyRequired(0);
        defaultConfig.setMaxTicketsPerUser(4);
        defaultConfig.setRefundPolicyType(1);
        defaultConfig.setTier1FreeRefundHours(48);
        defaultConfig.setIsWaitingAllowed(0);
        defaultConfig.setIsTransferable(0);
        eventConfigService.save(defaultConfig);

        // 缓存预热
        warmUpEventCache(eventDO, defaultConfig);

        // 将运行时生成的演出ID放入日志上下文，供 @LogRecord 注解解析 bizNo
        LogRecordContext.putVariable("bizNo", eventDO.getId());
    }

    @Override
    public IPage<EventPageQueryRespDTO> pageQueryEvents(EventPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<EventDO> queryWrapper = Wrappers.lambdaQuery(EventDO.class)
                .eq(requestParam.getStatus() != null, EventDO::getStatus, requestParam.getStatus())
                .orderByDesc(EventDO::getId);
        IPage<EventDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage.convert(each -> {
            EventPageQueryRespDTO dto = BeanUtil.toBean(each, EventPageQueryRespDTO.class);
            // 级联查询获取出演艺人
            try {
                List<Long> pIds = jdbcTemplate.queryForList("SELECT performer_id FROM t_event_performer WHERE event_id = ?",
                        Long.class, each.getId());
                if (pIds != null && !pIds.isEmpty()) {
                    Long pId = pIds.get(0);
                    dto.setPerformerId(pId);
                    List<String> pNames = jdbcTemplate.queryForList("SELECT name FROM t_performer WHERE id = ?",
                            String.class, pId);
                    if (pNames != null && !pNames.isEmpty()) {
                        dto.setPerformerName(pNames.get(0));
                    }
                }
            } catch (Exception e) {
                log.error("级联查询演出歌手名称失败, eventId={}", each.getId(), e);
            }
            return dto;
        });
    }

    @Override
    public EventQueryRespDTO getEventById(Long id) {
        EventDO eventDO = getById(id);
        if (eventDO == null) {
            return null;
        }
        EventQueryRespDTO dto = BeanUtil.toBean(eventDO, EventQueryRespDTO.class);
        // 级联查询获取出演艺人
        try {
            List<Long> pIds = jdbcTemplate.queryForList("SELECT performer_id FROM t_event_performer WHERE event_id = ?",
                    Long.class, id);
            if (pIds != null && !pIds.isEmpty()) {
                Long pId = pIds.get(0);
                dto.setPerformerId(pId);
                List<String> pNames = jdbcTemplate.queryForList("SELECT name FROM t_performer WHERE id = ?",
                        String.class, pId);
                if (pNames != null && !pNames.isEmpty()) {
                    dto.setPerformerName(pNames.get(0));
                }
            }
        } catch (Exception e) {
            log.error("级联查询详情演出歌手名称失败, eventId={}", id, e);
        }
        return dto;
    }

    @LogRecord(
            success = "修改演出信息：演出ID {{#requestParam.id}}",
            type = "Event",
            bizNo = "{{#requestParam.id}}",
            extra = "{{#requestParam.toString()}}"
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateEvent(EventUpdateReqDTO requestParam) {
        // 保存修改前的原始数据到日志上下文
        EventDO originalEvent = getById(requestParam.getId());
        if (originalEvent != null) {
            LogRecordContext.putVariable("originalData", JSON.toJSONString(originalEvent));
        }

        EventDO eventDO = BeanUtil.toBean(requestParam, EventDO.class);
        updateById(eventDO);

        // 级联更新艺人关联
        jdbcTemplate.update("DELETE FROM t_event_performer WHERE event_id = ?", requestParam.getId());
        if (requestParam.getPerformerId() != null) {
            jdbcTemplate.update("INSERT IGNORE INTO t_event_performer (event_id, performer_id) VALUES (?, ?)",
                    requestParam.getId(), requestParam.getPerformerId());
        }

        // 重新捞取最新全量数据刷新缓存
        EventDO latestEvent = getById(requestParam.getId());
        EventConfigDO latestConfig = eventConfigService.getByEventId(requestParam.getId());
        if (latestEvent != null && latestConfig != null) {
            warmUpEventCache(latestEvent, latestConfig);
            log.info("演出修改完成 & 缓存已同步刷新 | eventId={}", requestParam.getId());
        }
    }

    @LogRecord(
            success = "删除演出：演出ID {{#id}}",
            type = "Event",
            bizNo = "{{#id}}"
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteEvent(Long id) {
        // 保存删除前的原始数据到日志上下文
        EventDO originalEvent = getById(id);
        if (originalEvent != null) {
            LogRecordContext.putVariable("originalData", JSON.toJSONString(originalEvent));
        }

        LambdaQueryWrapper<EventConfigDO> configQuery = Wrappers.lambdaQuery(EventConfigDO.class)
                .eq(EventConfigDO::getEventId, id);
        eventConfigService.remove(configQuery);
        removeById(id);

        // 级联删除艺人关联关系
        jdbcTemplate.update("DELETE FROM t_event_performer WHERE event_id = ?", id);

        // 同步清除 Redis 缓存
        try {
            stringRedisTemplate.delete(String.format(MerchantAdminRedisConstant.EVENT_DETAIL_KEY, id));
            log.info("演出删除完成 & 缓存已清除 | eventId={}", id);
        } catch (Exception e) {
            log.error("演出缓存清除失败（非阻塞） | eventId={}", id, e);
        }
    }

    @LogRecord(
            success = "演出上架开售：演出ID {{#id}}，状态变更为 {COMMON_ENUM_PARSE{'EventStatusEnum_2'}}",
            type = "Event",
            bizNo = "{{#id}}"
    )
    @Override
    public void publishEvent(Long id) {
        EventDO event = getById(id);
        if (event == null) {
            throw new ClientException("演出不存在");
        }
        if (ObjectUtil.notEqual(event.getStatus(), EventStatusEnum.PRESALE.getStatus())) {
            throw new ClientException("仅预售状态的演出可以上架开售");
        }

        EventDO update = new EventDO();
        update.setId(id);
        update.setStatus(EventStatusEnum.ON_SALE.getStatus());
        updateById(update);

        syncCacheField(id, "status", String.valueOf(EventStatusEnum.ON_SALE.getStatus()));
        log.info("演出已上架开售 | eventId={}", id);
    }

    @LogRecord(
            success = "演出下架：演出ID {{#id}}，状态变更为 {COMMON_ENUM_PARSE{'EventStatusEnum_0'}}",
            type = "Event",
            bizNo = "{{#id}}"
    )
    @Override
    public void shelveEvent(Long id) {
        EventDO event = getById(id);
        if (event == null) {
            throw new ClientException("演出不存在");
        }
        if (ObjectUtil.notEqual(event.getStatus(), EventStatusEnum.ON_SALE.getStatus())) {
            throw new ClientException("仅在售状态的演出可以下架");
        }

        EventDO update = new EventDO();
        update.setId(id);
        update.setStatus(EventStatusEnum.OFF_SHELF.getStatus());
        updateById(update);

        syncCacheField(id, "status", String.valueOf(EventStatusEnum.OFF_SHELF.getStatus()));
        log.info("演出已下架 | eventId={}", id);
    }

    @LogRecord(
            success = "终止演出售票：演出ID {{#id}}，状态变更为 {COMMON_ENUM_PARSE{'EventStatusEnum_0'}}",
            type = "Event",
            bizNo = "{{#id}}"
    )
    @Override
    public void terminateEvent(Long id) {
        EventDO event = getById(id);
        if (event == null) {
            throw new ClientException("演出不存在");
        }
        if (ObjectUtil.equal(event.getStatus(), EventStatusEnum.OFF_SHELF.getStatus())) {
            throw new ClientException("演出已处于下架状态，无需终止");
        }

        EventDO update = new EventDO();
        update.setId(id);
        update.setStatus(EventStatusEnum.OFF_SHELF.getStatus());
        updateById(update);

        syncCacheField(id, "status", String.valueOf(EventStatusEnum.OFF_SHELF.getStatus()));
        log.info("演出已终止售票 | eventId={}", id);
    }

    /**
     * 同步刷新 Redis Hash 中的单个字段
     */
    private void syncCacheField(Long eventId, String field, String value) {
        try {
            String cacheKey = String.format(MerchantAdminRedisConstant.EVENT_DETAIL_KEY, eventId);
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
            String eventCacheKey = String.format(MerchantAdminRedisConstant.EVENT_DETAIL_KEY, event.getId());
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
