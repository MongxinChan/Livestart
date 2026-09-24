package com.mongxin.livestart.distribution.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.distribution.common.constant.DistributionRedisConstant;
import com.mongxin.livestart.distribution.common.enums.EventSaleStageStatusEnum;
import com.mongxin.livestart.distribution.common.enums.EventStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dao.entity.EventSaleStageDO;
import com.mongxin.livestart.distribution.dao.entity.EventSaleStageSkuDO;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.EventSaleStageMapper;
import com.mongxin.livestart.distribution.dao.mapper.EventSaleStageSkuMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.dto.req.SaleStageParamDTO;
import com.mongxin.livestart.distribution.dto.req.SaleStageSkuParamDTO;
import com.mongxin.livestart.distribution.dto.req.TicketSkuParam;
import com.mongxin.livestart.distribution.dto.resp.SaleStagePreviewRespDTO;
import com.mongxin.livestart.distribution.dto.resp.SaleStageRespDTO;
import com.mongxin.livestart.distribution.service.EventService;
import com.mongxin.livestart.distribution.service.XxlJobApiService;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Event publishing service with multi-stage ticket release support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {

    private final TicketSkuMapper ticketSkuMapper;
    private final EventSaleStageMapper eventSaleStageMapper;
    private final EventSaleStageSkuMapper eventSaleStageSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final XxlJobApiService xxlJobApiService;

    @Override
    public SaleStagePreviewRespDTO getNextSaleStage(Long sourceEventId) {
        EventDO distributionEvent = getOne(Wrappers.lambdaQuery(EventDO.class)
                .eq(EventDO::getSourceEventId, sourceEventId)
                .orderByDesc(EventDO::getCreateTime)
                .last("LIMIT 1"));
        if (distributionEvent == null) {
            return null;
        }
        EventSaleStageDO stage = eventSaleStageMapper.selectOne(Wrappers.lambdaQuery(EventSaleStageDO.class)
                .eq(EventSaleStageDO::getEventId, distributionEvent.getId())
                .eq(EventSaleStageDO::getStatus, EventSaleStageStatusEnum.PENDING.getCode())
                .gt(EventSaleStageDO::getSaleStartTime, new Date())
                .orderByAsc(EventSaleStageDO::getSaleStartTime)
                .last("LIMIT 1"));
        if (stage == null) {
            return null;
        }
        SaleStagePreviewRespDTO result = new SaleStagePreviewRespDTO();
        result.setEventId(distributionEvent.getId());
        result.setId(stage.getId());
        result.setStageName(stage.getStageName());
        result.setSaleStartTime(stage.getSaleStartTime());
        return result;
    }

    @Override
    public List<SaleStageRespDTO> listSaleStages(Long eventId) {
        return eventSaleStageMapper.selectList(Wrappers.lambdaQuery(EventSaleStageDO.class)
                        .eq(EventSaleStageDO::getEventId, eventId)
                        .orderByAsc(EventSaleStageDO::getStageNo))
                .stream()
                .map(stage -> {
                    SaleStageRespDTO result = new SaleStageRespDTO();
                    result.setId(stage.getId());
                    result.setEventId(stage.getEventId());
                    result.setStageNo(stage.getStageNo());
                    result.setStageName(stage.getStageName());
                    result.setSaleStartTime(stage.getSaleStartTime());
                    result.setStatus(stage.getStatus());
                    return result;
                })
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishEvent(EventPublishReqDTO requestParam) {
        validatePublishRequest(requestParam);
        if (getOne(Wrappers.lambdaQuery(EventDO.class)
                .eq(EventDO::getSourceEventId, requestParam.getSourceEventId())
                .last("LIMIT 1")) != null) {
            return;
        }

        Date earliestSaleStartTime = resolveEarliestSaleStartTime(requestParam);
        boolean immediateRelease = earliestSaleStartTime == null || !earliestSaleStartTime.after(new Date());

        EventDO eventDO = EventDO.builder()
                .sourceEventId(requestParam.getSourceEventId())
                .title(requestParam.getTitle())
                .artistId(requestParam.getArtistId())
                .artistName(requestParam.getArtistName())
                .venueId(requestParam.getVenueId())
                .eventType(resolveEventType(requestParam.getVenueId()))
                .startTime(requestParam.getEventTime())
                .eventTime(requestParam.getEventTime())
                .saleStartTime(earliestSaleStartTime)
                .status(immediateRelease ? EventStatusEnum.ON_SALE.getCode() : EventStatusEnum.PENDING_SALE.getCode())
                .build();

        if (!save(eventDO)) {
            throw new ServiceException("Failed to save event");
        }

        Map<String, TicketSkuDO> savedSkuMap = saveTicketSkus(eventDO.getId(), requestParam.getSkus(), immediateRelease);
        List<EventSaleStageDO> savedStages = saveSaleStages(eventDO.getId(), requestParam.getSaleStages(), savedSkuMap);

        if (!immediateRelease && CollUtil.isNotEmpty(savedStages)) {
            registerEarliestStageJob(eventDO, savedStages);
        }

        log.info("[Event Publish] Event published successfully. eventId={}, artistName={}, stageCount={}, releaseMode={}",
                eventDO.getId(), eventDO.getArtistName(), savedStages.size(), immediateRelease ? "IMMEDIATE" : "SCHEDULED");
    }

    private void validatePublishRequest(EventPublishReqDTO requestParam) {
        if (CollUtil.isEmpty(requestParam.getSkus())) {
            throw new ServiceException("Ticket sku config cannot be empty");
        }
        if (CollUtil.isEmpty(requestParam.getSaleStages())) {
            throw new ServiceException("Sale stages cannot be empty");
        }
    }

    private Map<String, TicketSkuDO> saveTicketSkus(Long eventId, List<TicketSkuParam> skus, boolean immediateRelease) {
        Map<String, TicketSkuDO> skuMap = new HashMap<>(skus.size());
        for (TicketSkuParam skuParam : skus) {
            TicketSkuDO skuDO = TicketSkuDO.builder()
                    .eventId(eventId)
                    .title(skuParam.getTitle())
                    .originalPrice(skuParam.getSellingPrice())
                    .sellingPrice(skuParam.getSellingPrice())
                    .totalStock(skuParam.getTotalStock())
                    .remainingStock(immediateRelease ? skuParam.getTotalStock() : 0)
                    .limitNum(skuParam.getLimitNum() != null ? skuParam.getLimitNum() : 2)
                    .version(0)
                    .build();

            if (ticketSkuMapper.insert(skuDO) <= 0) {
                throw new ServiceException("Failed to save ticket sku");
            }

            if (immediateRelease) {
                preheatRedisStock(skuDO);
            }
            skuMap.put(skuDO.getTitle(), skuDO);
        }
        return skuMap;
    }

    private List<EventSaleStageDO> saveSaleStages(Long eventId,
                                                  List<SaleStageParamDTO> saleStages,
                                                  Map<String, TicketSkuDO> savedSkuMap) {
        List<SaleStageParamDTO> sortedStages = saleStages.stream()
                .sorted(Comparator.comparing(SaleStageParamDTO::getSaleStartTime)
                        .thenComparing(SaleStageParamDTO::getStageNo))
                .toList();
        List<EventSaleStageDO> savedStages = new ArrayList<>(sortedStages.size());

        for (SaleStageParamDTO stageParam : sortedStages) {
            EventSaleStageDO stageDO = EventSaleStageDO.builder()
                    .eventId(eventId)
                    .stageNo(stageParam.getStageNo())
                    .stageName(stageParam.getStageName())
                    .saleStartTime(stageParam.getSaleStartTime())
                    .status(resolveStageStatus(stageParam.getSaleStartTime()))
                    .remark(stageParam.getRemark())
                    .build();

            if (eventSaleStageMapper.insert(stageDO) <= 0) {
                throw new ServiceException("Failed to save sale stage");
            }

            saveStageSkuConfigs(eventId, stageDO.getId(), stageParam.getSkuConfigs(), savedSkuMap);
            savedStages.add(stageDO);
        }
        return savedStages;
    }

    private void saveStageSkuConfigs(Long eventId,
                                     Long stageId,
                                     List<SaleStageSkuParamDTO> skuConfigs,
                                     Map<String, TicketSkuDO> savedSkuMap) {
        for (SaleStageSkuParamDTO skuConfig : skuConfigs) {
            TicketSkuDO ticketSkuDO = savedSkuMap.get(skuConfig.getSkuTitle());
            if (ticketSkuDO == null) {
                throw new ServiceException("Sale stage references unknown sku title: " + skuConfig.getSkuTitle());
            }

            EventSaleStageSkuDO stageSkuDO = EventSaleStageSkuDO.builder()
                    .stageId(stageId)
                    .eventId(eventId)
                    .ticketSkuId(ticketSkuDO.getId())
                    .releaseStock(skuConfig.getReleaseStock())
                    .releasedFlag(0)
                    .build();
            if (eventSaleStageSkuMapper.insert(stageSkuDO) <= 0) {
                throw new ServiceException("Failed to save sale stage sku config");
            }
        }
    }

    private void registerEarliestStageJob(EventDO eventDO, List<EventSaleStageDO> savedStages) {
        EventSaleStageDO earliestStage = savedStages.stream()
                .min(Comparator.comparing(EventSaleStageDO::getSaleStartTime)
                        .thenComparing(EventSaleStageDO::getStageNo))
                .orElseThrow(() -> new ServiceException("No sale stage saved"));

        Integer jobId = null;
        try {
            String cronExpression = dateToCron(earliestStage.getSaleStartTime());
            jobId = xxlJobApiService.addTicketReleaseJob(eventDO.getId(), eventDO.getTitle(), cronExpression);

            EventDO eventUpdateDO = new EventDO();
            eventUpdateDO.setId(eventDO.getId());
            eventUpdateDO.setXxlJobId(jobId);
            if (!updateById(eventUpdateDO)) {
                throw new ServiceException("保存演出开售任务失败");
            }

            EventSaleStageDO stageUpdateDO = new EventSaleStageDO();
            stageUpdateDO.setId(earliestStage.getId());
            stageUpdateDO.setXxlJobId(jobId);
            if (eventSaleStageMapper.updateById(stageUpdateDO) <= 0) {
                throw new ServiceException("保存开售阶段任务失败");
            }

            log.info("[Event Publish] Registered earliest stage release job. eventId={}, stageId={}, jobId={}, saleStartTime={}",
                    eventDO.getId(), earliestStage.getId(), jobId, earliestStage.getSaleStartTime());
        } catch (Exception e) {
            log.error("[Event Publish] Failed to register scheduled release job. eventId={}", eventDO.getId(), e);
            if (jobId != null) {
                xxlJobApiService.removeJob(jobId);
            }
            throw new ServiceException("开售任务注册失败，演出未发布");
        }
    }

    private void preheatRedisStock(TicketSkuDO skuDO) {
        String redisKey = String.format(DistributionRedisConstant.TICKET_STOCK_KEY, skuDO.getId());
        try {
            stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(skuDO.getRemainingStock()));
            log.info("[Ticket Publish] Preheated Redis stock. key={}, stock={}", redisKey, skuDO.getRemainingStock());
        } catch (Exception e) {
            log.error("[Ticket Publish] Failed to preheat Redis stock. key={}", redisKey, e);
            throw new ServiceException("Failed to preheat Redis stock");
        }
    }

    private Date resolveEarliestSaleStartTime(EventPublishReqDTO requestParam) {
        if (CollUtil.isNotEmpty(requestParam.getSaleStages())) {
            return requestParam.getSaleStages().stream()
                    .map(SaleStageParamDTO::getSaleStartTime)
                    .filter(date -> date != null)
                    .min(Date::compareTo)
                    .orElse(requestParam.getSaleStartTime());
        }
        return requestParam.getSaleStartTime();
    }

    private Integer resolveStageStatus(Date saleStartTime) {
        if (saleStartTime == null || !saleStartTime.after(new Date())) {
            return EventSaleStageStatusEnum.OPENED.getCode();
        }
        return EventSaleStageStatusEnum.PENDING.getCode();
    }

    /**
     * Convert a timestamp to an XXL-JOB-compatible cron expression with second precision.
     */
    private String dateToCron(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.format("%d %d %d %d %d ? %d",
                cal.get(Calendar.SECOND),
                cal.get(Calendar.MINUTE),
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR));
    }

    /**
     * Keep distribution-created events compatible with the shared event schema.
     * Large stadiums default to concert mode, small venues default to livehouse mode.
     */
    private int resolveEventType(Long venueId) {
        return venueId != null && venueId >= 101001L && venueId <= 101005L ? 1 : 0;
    }
}
