package com.mongxin.livestart.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
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
import com.mongxin.livestart.merchant.admin.dao.entity.EventStyleRelationDO;
import com.mongxin.livestart.merchant.admin.dao.entity.StyleDO;
import com.mongxin.livestart.merchant.admin.dao.entity.TicketSkuDO;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.EventStyleRelationMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.StyleMapper;
import com.mongxin.livestart.merchant.admin.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.merchant.admin.dto.req.EventImportExcelDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventConfigUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.SaleStageReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.ImportResultRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.SaleStageRespDTO;
import com.mongxin.livestart.merchant.admin.remote.DistributionRemoteService;
import com.mongxin.livestart.merchant.admin.remote.dto.DistributionEventPublishReqDTO;
import com.mongxin.livestart.merchant.admin.remote.dto.DistributionSaleStageParamDTO;
import com.mongxin.livestart.merchant.admin.remote.dto.DistributionSaleStageSkuParamDTO;
import com.mongxin.livestart.merchant.admin.remote.dto.DistributionTicketSkuParamDTO;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import com.mongxin.livestart.merchant.admin.service.EventService;
import com.mongxin.livestart.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import com.mongxin.livestart.merchant.admin.toolkit.EasyExcelImportUtil;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mongxin.livestart.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_EVENT_KEY;

/**
 * 演出服务实现层
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {

    private static final String ENGINE_TICKET_STOCK_KEY = "engine:stock:sku:%d";

    private final EventConfigService eventConfigService;
    private final StringRedisTemplate stringRedisTemplate;
    private final MerchantAdminChainContext merchantAdminChainContext;
    private final JdbcTemplate jdbcTemplate;
    private final EventStyleRelationMapper eventStyleRelationMapper;
    private final StyleMapper styleMapper;
    private final TicketSkuMapper ticketSkuMapper;
    private final DistributionRemoteService distributionRemoteService;

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
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `t_event_ticket_stage` (\n" +
                    "  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                    "  `event_id` bigint NOT NULL COMMENT '演出ID',\n" +
                    "  `ticket_stage` tinyint(1) NOT NULL DEFAULT '1' COMMENT '开票阶段 1:一开 2:二开',\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  UNIQUE KEY `idx_event_stage` (`event_id`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出开票阶段单表';");
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS `t_event_sale_stage_config` (\n" +
                    "  `id` bigint NOT NULL AUTO_INCREMENT,\n" +
                    "  `event_id` bigint NOT NULL COMMENT '演出ID',\n" +
                    "  `stage_no` tinyint NOT NULL COMMENT '阶段序号',\n" +
                    "  `stage_name` varchar(64) NOT NULL COMMENT '阶段名称',\n" +
                    "  `sale_start_time` datetime NOT NULL COMMENT '阶段开售时间',\n" +
                    "  `remark` varchar(255) DEFAULT NULL COMMENT '备注',\n" +
                    "  PRIMARY KEY (`id`),\n" +
                    "  UNIQUE KEY `uk_event_stage_no` (`event_id`,`stage_no`)\n" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出开售阶段配置表';");
            log.info("[merchant-admin] 演出关联表与开售阶段配置表校验完成");
        } catch (Exception e) {
            log.error("[merchant-admin] 初始化关联表失败", e);
        }
    }

    @LogRecord(success = """
            创建演出：{{#requestParam.title}}；
            演出类型：{{#requestParam.eventType == 0 ? 'Livehouse(站票)' : '演唱会(选座)'}}；
            关联场馆ID：{{#requestParam.venueId}}；
            演出时间：{{#requestParam.startTime}};
            """, type = "Event", bizNo = "{{#bizNo}}", extra = "{{#requestParam.toString()}}")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createEvent(EventSaveReqDTO requestParam) {
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_EVENT_KEY.name(), requestParam);

        EventDO eventDO = BeanUtil.toBean(requestParam, EventDO.class);
        eventDO.setStatus(EventStatusEnum.PRESALE.getStatus());
        save(eventDO);

        savePerformerRelation(eventDO.getId(), requestParam.getPerformerId());
        saveStyleRelations(eventDO.getId(), requestParam.getStyleIds());
        saveSaleStages(eventDO.getId(), requestParam.getSaleStages(), requestParam.getTicketStage());

        EventConfigUpdateReqDTO defaultConfigRequest = new EventConfigUpdateReqDTO();
        defaultConfigRequest.setEventId(eventDO.getId());
        defaultConfigRequest.setSelectionMode(0);
        defaultConfigRequest.setIsVerifyRequired(0);
        defaultConfigRequest.setMaxTicketsPerUser(4);
        defaultConfigRequest.setRefundPolicyType(1);
        defaultConfigRequest.setTier1FreeRefundHours(48);
        defaultConfigRequest.setIsWaitingAllowed(0);
        defaultConfigRequest.setIsTransferable(0);
        eventConfigService.saveOrUpdateConfig(defaultConfigRequest);

        EventConfigDO defaultConfig = BeanUtil.toBean(defaultConfigRequest, EventConfigDO.class);

        warmUpEventCache(eventDO, defaultConfig);
        LogRecordContext.putVariable("bizNo", eventDO.getId());
    }

    @Override
    public ImportResultRespDTO importEvents(MultipartFile file) {
        List<EventImportExcelDTO> rows = EasyExcelImportUtil.readFirstSheet(file, EventImportExcelDTO.class);
        ImportResultRespDTO result = new ImportResultRespDTO();
        if (rows.isEmpty()) {
            result.addFail(1, "Excel 没有可导入的数据行，请保留表头并从第 2 行开始填写");
            return result;
        }
        for (int i = 0; i < rows.size(); i++) {
            int rowIndex = i + 2;
            try {
                EventImportExcelDTO row = rows.get(i);
                validateEventImportRow(row);
                EventSaveReqDTO requestParam = new EventSaveReqDTO();
                requestParam.setTitle(row.getTitle().trim());
                requestParam.setEventType(row.getEventType());
                requestParam.setVenueId(row.getVenueId());
                requestParam.setPerformerId(row.getPerformerId());
                requestParam.setStartTime(row.getStartTime());
                requestParam.setPosterUrl(row.getPosterUrl() == null ? null : row.getPosterUrl().trim());
                requestParam.setTicketStage(row.getTicketStage() == null ? 1 : row.getTicketStage());
                createEvent(requestParam);
                result.addSuccess();
            } catch (Exception ex) {
                result.addFail(rowIndex, ex.getMessage());
            }
        }
        return result;
    }

    private void validateEventImportRow(EventImportExcelDTO row) {
        if (row == null) {
            throw new ClientException("空行不能导入");
        }
        if (row.getTitle() == null || row.getTitle().isBlank()) {
            throw new ClientException("title 不能为空");
        }
        if (row.getEventType() == null) {
            throw new ClientException("eventType 不能为空");
        }
        if (row.getEventType() != 0 && row.getEventType() != 1) {
            throw new ClientException("eventType 只能填写 0 或 1");
        }
        if (row.getVenueId() == null) {
            throw new ClientException("venueId 不能为空");
        }
        if (row.getStartTime() == null) {
            throw new ClientException("startTime 不能为空，格式为 yyyy-MM-dd HH:mm:ss");
        }
        if (row.getTicketStage() != null && row.getTicketStage() != 1 && row.getTicketStage() != 2) {
            throw new ClientException("ticketStage 只能填写 1 或 2");
        }
    }

    @Override
    public IPage<EventPageQueryRespDTO> pageQueryEvents(EventPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<EventDO> queryWrapper = Wrappers.lambdaQuery(EventDO.class)
                .eq(requestParam.getStatus() != null, EventDO::getStatus, requestParam.getStatus())
                .orderByDesc(EventDO::getId);
        IPage<EventDO> selectPage = baseMapper.selectPage(requestParam, queryWrapper);
        return selectPage
                .convert(each -> enrichEventPageResp(BeanUtil.toBean(each, EventPageQueryRespDTO.class), each.getId()));
    }

    @Override
    public EventQueryRespDTO getEventById(Long id) {
        EventDO eventDO = getById(id);
        if (eventDO == null) {
            return null;
        }
        return enrichEventDetailResp(BeanUtil.toBean(eventDO, EventQueryRespDTO.class), id);
    }

    @LogRecord(success = "修改演出信息：演出ID {{#requestParam.id}}", type = "Event", bizNo = "{{#requestParam.id}}", extra = "{{#requestParam.toString()}}")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateEvent(EventUpdateReqDTO requestParam) {
        EventDO originalEvent = getById(requestParam.getId());
        Integer originalTicketStage = getTicketStage(requestParam.getId());
        if (originalEvent != null) {
            LogRecordContext.putVariable("originalData", JSON.toJSONString(originalEvent));
        }

        EventDO eventDO = BeanUtil.toBean(requestParam, EventDO.class);
        updateById(eventDO);

        savePerformerRelation(requestParam.getId(), requestParam.getPerformerId());
        saveStyleRelations(requestParam.getId(), requestParam.getStyleIds());
        saveSaleStages(requestParam.getId(), requestParam.getSaleStages(), requestParam.getTicketStage());

        List<SaleStageRespDTO> respStages = CollUtil.isNotEmpty(requestParam.getSaleStages())
                ? toRespStages(requestParam.getSaleStages())
                : new ArrayList<>();
        Integer currentTicketStage = resolveTicketStageFromSaleStages(respStages, requestParam.getTicketStage());
        releaseStage2StockIfNeeded(requestParam.getId(), originalTicketStage, currentTicketStage);

        EventDO latestEvent = getById(requestParam.getId());
        EventConfigDO latestConfig = eventConfigService.getByEventId(requestParam.getId());
        if (latestEvent != null && latestConfig != null) {
            warmUpEventCache(latestEvent, latestConfig);
            log.info("演出修改完成并刷新缓存 | eventId={}", requestParam.getId());
        }
    }

    @LogRecord(success = "删除演出：演出ID {{#id}}", type = "Event", bizNo = "{{#id}}")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteEvent(Long id) {
        EventDO originalEvent = getById(id);
        if (originalEvent != null) {
            LogRecordContext.putVariable("originalData", JSON.toJSONString(originalEvent));
        }

        LambdaQueryWrapper<EventConfigDO> configQuery = Wrappers.lambdaQuery(EventConfigDO.class)
                .eq(EventConfigDO::getEventId, id);
        eventConfigService.remove(configQuery);
        removeById(id);

        jdbcTemplate.update("DELETE FROM t_event_performer WHERE event_id = ?", id);
        jdbcTemplate.update("DELETE FROM t_event_ticket_stage WHERE event_id = ?", id);
        jdbcTemplate.update("DELETE FROM t_event_sale_stage_config WHERE event_id = ?", id);
        eventStyleRelationMapper.delete(
                Wrappers.lambdaQuery(EventStyleRelationDO.class)
                        .eq(EventStyleRelationDO::getEventId, id));

        try {
            stringRedisTemplate.delete(String.format(MerchantAdminRedisConstant.EVENT_DETAIL_KEY, id));
            log.info("演出删除完成并清理缓存 | eventId={}", id);
        } catch (Exception e) {
            log.error("演出缓存清理失败 | eventId={}", id, e);
        }
    }

    @LogRecord(success = "演出上架开售：演出ID {{#id}}，状态变更为 {COMMON_ENUM_PARSE{'EventStatusEnum_2'}}", type = "Event", bizNo = "{{#id}}")
    @Override
    public void publishEvent(Long id) {
        EventDO event = getById(id);
        if (event == null) {
            throw new ClientException("演出不存在");
        }
        if (ObjectUtil.notEqual(event.getStatus(), EventStatusEnum.PRESALE.getStatus())) {
            throw new ClientException("仅预售状态的演出可以上架开售");
        }

        DistributionEventPublishReqDTO publishReqDTO = buildDistributionPublishReq(event);
        distributionRemoteService.publishEvent(publishReqDTO);

        EventDO update = new EventDO();
        update.setId(id);
        update.setStatus(EventStatusEnum.ON_SALE.getStatus());
        updateById(update);

        syncCacheField(id, "status", String.valueOf(EventStatusEnum.ON_SALE.getStatus()));
        log.info("演出已上架开售 | eventId={}", id);
    }

    @LogRecord(success = "演出下架：演出ID {{#id}}，状态变更为 {COMMON_ENUM_PARSE{'EventStatusEnum_0'}}", type = "Event", bizNo = "{{#id}}")
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

    @LogRecord(success = "终止演出售票：演出ID {{#id}}，状态变更为 {COMMON_ENUM_PARSE{'EventStatusEnum_0'}}", type = "Event", bizNo = "{{#id}}")
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

    private EventPageQueryRespDTO enrichEventPageResp(EventPageQueryRespDTO dto, Long eventId) {
        fillPerformerInfo(dto, eventId);
        dto.setSaleStages(querySaleStages(eventId));
        dto.setTicketStage(resolveTicketStageFromSaleStages(dto.getSaleStages(), getTicketStage(eventId)));
        fillStyleInfo(dto, eventId);
        return dto;
    }

    private EventQueryRespDTO enrichEventDetailResp(EventQueryRespDTO dto, Long eventId) {
        fillPerformerInfo(dto, eventId);
        dto.setSaleStages(querySaleStages(eventId));
        dto.setTicketStage(resolveTicketStageFromSaleStages(dto.getSaleStages(), getTicketStage(eventId)));
        fillStyleInfo(dto, eventId);
        return dto;
    }

    private void fillPerformerInfo(Object dto, Long eventId) {
        try {
            List<Long> pIds = jdbcTemplate.queryForList("SELECT performer_id FROM t_event_performer WHERE event_id = ?",
                    Long.class, eventId);
            if (pIds != null && !pIds.isEmpty()) {
                Long pId = pIds.get(0);
                BeanUtil.setFieldValue(dto, "performerId", pId);
                List<String> pNames = jdbcTemplate.queryForList("SELECT name FROM t_performer WHERE id = ?",
                        String.class, pId);
                if (pNames != null && !pNames.isEmpty()) {
                    BeanUtil.setFieldValue(dto, "performerName", pNames.get(0));
                }
            }
        } catch (Exception e) {
            log.error("查询演出艺人失败, eventId={}", eventId, e);
        }
    }

    private void fillStyleInfo(Object dto, Long eventId) {
        try {
            List<EventStyleRelationDO> relations = eventStyleRelationMapper.selectList(
                    Wrappers.lambdaQuery(EventStyleRelationDO.class)
                            .eq(EventStyleRelationDO::getEventId, eventId));
            List<Long> styleIds = relations.stream().map(EventStyleRelationDO::getStyleId).collect(Collectors.toList());
            BeanUtil.setFieldValue(dto, "styleIds", styleIds);
            if (CollUtil.isNotEmpty(styleIds)) {
                List<StyleDO> styles = styleMapper.selectBatchIds(styleIds);
                if (CollUtil.isNotEmpty(styles)) {
                    String genreNames = styles.stream().map(StyleDO::getName).collect(Collectors.joining(","));
                    BeanUtil.setFieldValue(dto, "genre", genreNames);
                }
            }
        } catch (Exception e) {
            log.error("查询演出风格失败, eventId={}", eventId, e);
        }
    }

    private void savePerformerRelation(Long eventId, Long performerId) {
        jdbcTemplate.update("DELETE FROM t_event_performer WHERE event_id = ?", eventId);
        if (performerId != null) {
            jdbcTemplate.update("INSERT IGNORE INTO t_event_performer (event_id, performer_id) VALUES (?, ?)",
                    eventId, performerId);
        }
    }

    private void saveStyleRelations(Long eventId, List<Long> styleIds) {
        eventStyleRelationMapper.delete(
                Wrappers.lambdaQuery(EventStyleRelationDO.class)
                        .eq(EventStyleRelationDO::getEventId, eventId));
        if (CollUtil.isNotEmpty(styleIds)) {
            for (Long styleId : styleIds) {
                eventStyleRelationMapper.insert(EventStyleRelationDO.builder()
                        .eventId(eventId)
                        .styleId(styleId)
                        .build());
            }
        }
    }

    private void saveSaleStages(Long eventId, List<SaleStageReqDTO> saleStages, Integer fallbackTicketStage) {
        jdbcTemplate.update("DELETE FROM t_event_sale_stage_config WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM t_event_ticket_stage WHERE event_id = ?", eventId);

        List<SaleStageReqDTO> normalizedStages = normalizeSaleStages(saleStages, fallbackTicketStage);
        int ticketStage = resolveTicketStageFromSaleStages(toRespStages(normalizedStages), fallbackTicketStage);
        jdbcTemplate.update("INSERT IGNORE INTO t_event_ticket_stage (event_id, ticket_stage) VALUES (?, ?)",
                eventId, ticketStage);

        for (SaleStageReqDTO stage : normalizedStages) {
            jdbcTemplate.update(
                    "INSERT INTO t_event_sale_stage_config (event_id, stage_no, stage_name, sale_start_time, remark) VALUES (?, ?, ?, ?, ?)",
                    eventId,
                    stage.getStageNo(),
                    stage.getStageName(),
                    stage.getSaleStartTime(),
                    stage.getRemark());
        }
    }

    private List<SaleStageReqDTO> normalizeSaleStages(List<SaleStageReqDTO> saleStages, Integer fallbackTicketStage) {
        if (CollUtil.isNotEmpty(saleStages)) {
            return saleStages.stream()
                    .sorted(Comparator.comparing(SaleStageReqDTO::getStageNo, Comparator.nullsLast(Integer::compareTo)))
                    .map(stage -> {
                        SaleStageReqDTO normalized = new SaleStageReqDTO();
                        normalized.setStageNo(stage.getStageNo());
                        normalized.setStageName(stage.getStageName());
                        normalized.setSaleStartTime(stage.getSaleStartTime());
                        normalized.setRemark(stage.getRemark());
                        return normalized;
                    })
                    .toList();
        }

        SaleStageReqDTO defaultStage = new SaleStageReqDTO();
        defaultStage.setStageNo(fallbackTicketStage != null && fallbackTicketStage > 1 ? fallbackTicketStage : 1);
        defaultStage.setStageName(defaultStage.getStageNo() == 1 ? "预售第一阶段" : "预售第二阶段");
        defaultStage.setSaleStartTime(new java.util.Date());
        defaultStage.setRemark("兼容旧开票阶段数据自动生成");
        return List.of(defaultStage);
    }

    private List<SaleStageRespDTO> querySaleStages(Long eventId) {
        try {
            return jdbcTemplate.query(
                    "SELECT stage_no, stage_name, sale_start_time, remark FROM t_event_sale_stage_config WHERE event_id = ? ORDER BY stage_no ASC",
                    (rs, rowNum) -> toSaleStageResp(rs),
                    eventId);
        } catch (Exception e) {
            log.error("查询演出开售阶段失败, eventId={}", eventId, e);
            return new ArrayList<>();
        }
    }

    private SaleStageRespDTO toSaleStageResp(ResultSet rs) throws SQLException {
        SaleStageRespDTO dto = new SaleStageRespDTO();
        dto.setStageNo(rs.getInt("stage_no"));
        dto.setStageName(rs.getString("stage_name"));
        dto.setSaleStartTime(rs.getTimestamp("sale_start_time"));
        dto.setRemark(rs.getString("remark"));
        return dto;
    }

    private List<SaleStageRespDTO> toRespStages(List<SaleStageReqDTO> saleStages) {
        return saleStages.stream().map(each -> {
            SaleStageRespDTO dto = new SaleStageRespDTO();
            dto.setStageNo(each.getStageNo());
            dto.setStageName(each.getStageName());
            dto.setSaleStartTime(each.getSaleStartTime());
            dto.setRemark(each.getRemark());
            return dto;
        }).toList();
    }

    private int resolveTicketStageFromSaleStages(List<SaleStageRespDTO> saleStages, Integer fallbackTicketStage) {
        if (CollUtil.isNotEmpty(saleStages)) {
            return saleStages.stream()
                    .map(SaleStageRespDTO::getStageNo)
                    .filter(ObjectUtil::isNotNull)
                    .max(Integer::compareTo)
                    .orElse(fallbackTicketStage != null ? fallbackTicketStage : 1);
        }
        return fallbackTicketStage != null ? fallbackTicketStage : 1;
    }

    private DistributionEventPublishReqDTO buildDistributionPublishReq(EventDO event) {
        EventQueryRespDTO detail = getEventById(event.getId());
        if (detail == null) {
            throw new ClientException("演出详情不存在，无法发布");
        }
        if (detail.getPerformerId() == null || detail.getPerformerName() == null) {
            throw new ClientException("演出缺少艺人信息，无法同步发布到 distribution");
        }

        List<TicketSkuDO> ticketSkus = ticketSkuMapper.selectByEventId(event.getId());
        if (CollUtil.isEmpty(ticketSkus)) {
            throw new ClientException("演出缺少票档配置，无法发布");
        }

        DistributionEventPublishReqDTO requestDTO = new DistributionEventPublishReqDTO();
        requestDTO.setTitle(detail.getTitle());
        requestDTO.setArtistId(detail.getPerformerId());
        requestDTO.setArtistName(detail.getPerformerName());
        requestDTO.setEventTime(detail.getStartTime());
        requestDTO.setVenueId(detail.getVenueId());
        requestDTO.setSaleStartTime(resolveEarliestSaleStartTime(detail.getSaleStages()));
        requestDTO.setSkus(ticketSkus.stream().map(this::toDistributionSku).toList());
        requestDTO.setSaleStages(buildDistributionSaleStages(detail.getSaleStages(), ticketSkus));
        return requestDTO;
    }

    private DistributionTicketSkuParamDTO toDistributionSku(TicketSkuDO ticketSkuDO) {
        DistributionTicketSkuParamDTO skuParamDTO = new DistributionTicketSkuParamDTO();
        skuParamDTO.setTitle(ticketSkuDO.getTitle());
        skuParamDTO.setSellingPrice(ticketSkuDO.getSellingPrice());
        skuParamDTO.setTotalStock(ticketSkuDO.getTotalStock());
        skuParamDTO.setLimitNum(ticketSkuDO.getLimitNum());
        return skuParamDTO;
    }

    private List<DistributionSaleStageParamDTO> buildDistributionSaleStages(List<SaleStageRespDTO> saleStages,
            List<TicketSkuDO> ticketSkus) {
        if (CollUtil.isEmpty(saleStages)) {
            throw new ClientException("演出缺少开售阶段配置，无法发布");
        }

        return saleStages.stream()
                .sorted(Comparator.comparing(SaleStageRespDTO::getStageNo))
                .map(stage -> {
                    DistributionSaleStageParamDTO stageParamDTO = new DistributionSaleStageParamDTO();
                    stageParamDTO.setStageNo(stage.getStageNo());
                    stageParamDTO.setStageName(stage.getStageName());
                    stageParamDTO.setSaleStartTime(stage.getSaleStartTime());
                    stageParamDTO.setRemark(stage.getRemark());
                    stageParamDTO.setSkuConfigs(ticketSkus.stream()
                            .map(ticketSku -> toDistributionStageSku(stage.getStageNo(), ticketSku))
                            .toList());
                    return stageParamDTO;
                })
                .toList();
    }

    private DistributionSaleStageSkuParamDTO toDistributionStageSku(Integer stageNo, TicketSkuDO ticketSkuDO) {
        DistributionSaleStageSkuParamDTO skuParamDTO = new DistributionSaleStageSkuParamDTO();
        skuParamDTO.setSkuTitle(ticketSkuDO.getTitle());
        skuParamDTO.setReleaseStock(resolveReleaseStock(stageNo, ticketSkuDO));
        return skuParamDTO;
    }

    private Integer resolveReleaseStock(Integer stageNo, TicketSkuDO ticketSkuDO) {
        if (ObjectUtil.equal(stageNo, 1)) {
            return ticketSkuDO.getStage1Stock() == null ? ticketSkuDO.getTotalStock() : ticketSkuDO.getStage1Stock();
        }
        if (ObjectUtil.equal(stageNo, 2)) {
            return ticketSkuDO.getStage2Stock() == null ? 0 : ticketSkuDO.getStage2Stock();
        }
        return 0;
    }

    private java.util.Date resolveEarliestSaleStartTime(List<SaleStageRespDTO> saleStages) {
        return saleStages.stream()
                .map(SaleStageRespDTO::getSaleStartTime)
                .filter(ObjectUtil::isNotNull)
                .min(java.util.Date::compareTo)
                .orElse(null);
    }

    private void syncCacheField(Long eventId, String field, String value) {
        try {
            String cacheKey = String.format(MerchantAdminRedisConstant.EVENT_DETAIL_KEY, eventId);
            stringRedisTemplate.opsForHash().put(cacheKey, field, value);
        } catch (Exception e) {
            log.error("演出缓存字段同步失败 | eventId={} field={}", eventId, field, e);
        }
    }

    private Integer getTicketStage(Long eventId) {
        try {
            List<Integer> stages = jdbcTemplate.queryForList(
                    "SELECT ticket_stage FROM t_event_ticket_stage WHERE event_id = ?",
                    Integer.class,
                    eventId);
            return CollUtil.isNotEmpty(stages) ? stages.get(0) : 1;
        } catch (Exception e) {
            log.error("查询演出开票阶段失败 eventId={}", eventId, e);
            return 1;
        }
    }

    private void releaseStage2StockIfNeeded(Long eventId, Integer originalTicketStage, Integer currentTicketStage) {
        if (!ObjectUtil.equal(originalTicketStage, 1) || !ObjectUtil.equal(currentTicketStage, 2)) {
            return;
        }

        int affected = ticketSkuMapper.releaseStage2StockByEventId(eventId);
        if (affected <= 0) {
            log.info("演出切换到二开，无需释放二开库存 | eventId={}", eventId);
            return;
        }

        List<TicketSkuDO> ticketSkus = ticketSkuMapper.selectByEventId(eventId);
        for (TicketSkuDO each : ticketSkus) {
            syncTicketStockCache(each.getId(), each.getRemainingStock());
        }
        log.info("演出切换到二开，已释放二开库存 | eventId={} | releasedSkuCount={}", eventId, affected);
    }

    private void syncTicketStockCache(Long skuId, Integer stock) {
        try {
            String stockValue = String.valueOf(stock);
            String merchantKey = String.format(MerchantAdminRedisConstant.TICKET_STOCK_KEY, skuId);
            String engineKey = String.format(ENGINE_TICKET_STOCK_KEY, skuId);
            stringRedisTemplate.opsForValue().set(merchantKey, stockValue);
            stringRedisTemplate.opsForValue().set(engineKey, stockValue);
        } catch (Exception e) {
            log.error("同步票种库存缓存失败 | skuId={}", skuId, e);
        }
    }

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
                    args.toArray());
            log.info("演出缓存预热完成 | eventId={} | expireAt={}", event.getId(), expireTimeSec);
        } catch (Exception e) {
            log.error("演出缓存预热失败 | eventId={}", event.getId(), e);
        }
    }
}
