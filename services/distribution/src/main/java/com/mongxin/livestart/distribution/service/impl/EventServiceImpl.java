package com.mongxin.livestart.distribution.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mongxin.livestart.distribution.common.constant.DistributionRedisConstant;
import com.mongxin.livestart.distribution.common.enums.EventStatusEnum;
import com.mongxin.livestart.distribution.dao.entity.EventDO;
import com.mongxin.livestart.distribution.dao.entity.TicketSkuDO;
import com.mongxin.livestart.distribution.dao.mapper.EventMapper;
import com.mongxin.livestart.distribution.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.service.EventService;
import com.mongxin.livestart.distribution.service.XxlJobApiService;
import com.mongxin.livestart.framework.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;

/**
 * Event publishing service with immediate or scheduled ticket release support.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventServiceImpl extends ServiceImpl<EventMapper, EventDO> implements EventService {

    private final TicketSkuMapper ticketSkuMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final XxlJobApiService xxlJobApiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishEvent(EventPublishReqDTO requestParam) {
        boolean immediateRelease = requestParam.getSaleStartTime() == null
                || requestParam.getSaleStartTime().before(new Date());

        EventDO eventDO = EventDO.builder()
                .title(requestParam.getTitle())
                .artistId(requestParam.getArtistId())
                .artistName(requestParam.getArtistName())
                .venueId(requestParam.getVenueId())
                .eventType(resolveEventType(requestParam.getVenueId()))
                .startTime(requestParam.getEventTime())
                .eventTime(requestParam.getEventTime())
                .saleStartTime(requestParam.getSaleStartTime())
                .status(immediateRelease ? EventStatusEnum.ON_SALE.getCode() : EventStatusEnum.PENDING_SALE.getCode())
                .build();

        if (!save(eventDO)) {
            throw new ServiceException("Failed to save event");
        }

        if (CollUtil.isNotEmpty(requestParam.getSkus())) {
            for (EventPublishReqDTO.TicketSkuParam skuParam : requestParam.getSkus()) {
                TicketSkuDO skuDO = TicketSkuDO.builder()
                        .eventId(eventDO.getId())
                        .title(skuParam.getTitle())
                        .originalPrice(skuParam.getSellingPrice())
                        .sellingPrice(skuParam.getSellingPrice())
                        .totalStock(skuParam.getTotalStock())
                        .remainingStock(skuParam.getTotalStock())
                        .limitNum(skuParam.getLimitNum() != null ? skuParam.getLimitNum() : 2)
                        .version(0)
                        .build();

                int inserted = ticketSkuMapper.insert(skuDO);
                if (inserted <= 0) {
                    throw new ServiceException("Failed to save ticket sku");
                }

                if (immediateRelease) {
                    String redisKey = String.format(DistributionRedisConstant.TICKET_STOCK_KEY, skuDO.getId());
                    try {
                        stringRedisTemplate.opsForValue().set(redisKey, String.valueOf(skuDO.getRemainingStock()));
                        log.info("[Ticket Publish] Preheated Redis stock. key={}, stock={}",
                                redisKey, skuDO.getRemainingStock());
                    } catch (Exception e) {
                        log.error("[Ticket Publish] Failed to preheat Redis stock. key={}", redisKey, e);
                        throw new ServiceException("Failed to preheat Redis stock");
                    }
                }
            }
        }

        if (!immediateRelease) {
            try {
                String cronExpression = dateToCron(requestParam.getSaleStartTime());
                int jobId = xxlJobApiService.addTicketReleaseJob(eventDO.getId(), eventDO.getTitle(), cronExpression);

                EventDO updateDO = new EventDO();
                updateDO.setId(eventDO.getId());
                updateDO.setXxlJobId(jobId);
                updateById(updateDO);

                log.info("[Event Publish] Registered scheduled release job. eventId={}, jobId={}, saleStartTime={}",
                        eventDO.getId(), jobId, requestParam.getSaleStartTime());
            } catch (Exception e) {
                log.error("[Event Publish] Failed to register scheduled release job. eventId={}", eventDO.getId(), e);
                log.warn("[Event Publish] Event was saved, but XXL-JOB registration failed. Register it manually if needed.");
            }
        }

        log.info("[Event Publish] Event published successfully. eventId={}, artistName={}, releaseMode={}",
                eventDO.getId(), eventDO.getArtistName(), immediateRelease ? "IMMEDIATE" : "SCHEDULED");
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
