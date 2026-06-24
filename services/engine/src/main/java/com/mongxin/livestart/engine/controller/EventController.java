package com.mongxin.livestart.engine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.engine.dto.resp.EventListRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketSkuRespDTO;
import com.mongxin.livestart.engine.remote.MerchantAdminRemoteService;
import com.mongxin.livestart.engine.remote.dto.MerchantEventRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantVenueRespDTO;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "购票引擎 - 演出信息")
@RestController
@RequestMapping("/api/engine/event")
@RequiredArgsConstructor
public class EventController {

    private static final String UNKNOWN_VENUE = "未知场馆";
    private static final String UNKNOWN_CITY = "未知城市";

    private final MerchantAdminRemoteService merchantAdminRemoteService;

    @Operation(summary = "演出列表", description = "聚合 merchant-admin 的演出与票档信息，返回 C 端展示列表")
    @GetMapping("/list")
    public Result<List<EventListRespDTO>> listEvents() {
        log.info("[Engine] 拉取 C 端演出列表");

        Result<Page<MerchantEventRespDTO>> eventResult = merchantAdminRemoteService.pageQueryEvents(1, 50);
        if (eventResult.isFail() || eventResult.getData() == null) {
            log.warn("[Engine] 获取演出列表失败: {}", eventResult.getMessage());
            return Results.success(Collections.emptyList());
        }

        List<MerchantEventRespDTO> events = eventResult.getData().getRecords();
        if (events == null || events.isEmpty()) {
            return Results.success(Collections.emptyList());
        }

        Map<Long, List<MerchantTicketSkuRespDTO>> skuGroupByEvent = loadSkuGroup(null, 500);
        Map<Long, MerchantVenueRespDTO> venueCache = new HashMap<>();

        List<EventListRespDTO> resultList = new ArrayList<>();
        for (MerchantEventRespDTO event : events) {
            if (!isVisibleEvent(event)) {
                continue;
            }
            resultList.add(buildEventDetail(event, skuGroupByEvent, venueCache));
        }

        return Results.success(resultList);
    }

    @Operation(summary = "演出详情", description = "根据演出 ID 返回聚合后的详情与票档信息")
    @GetMapping("/{id}")
    public Result<EventListRespDTO> getEventDetail(@PathVariable("id") Long id) {
        log.info("[Engine] 拉取 C 端演出详情, eventId={}", id);

        Result<MerchantEventRespDTO> eventResult = merchantAdminRemoteService.getEvent(id);
        if (eventResult.isFail() || eventResult.getData() == null) {
            log.warn("[Engine] 获取演出详情失败, eventId={}, message={}", id, eventResult.getMessage());
            return Results.success(null);
        }

        MerchantEventRespDTO event = eventResult.getData();
        if (!isVisibleEvent(event)) {
            return Results.success(null);
        }

        Map<Long, List<MerchantTicketSkuRespDTO>> skuGroupByEvent = loadSkuGroup(id, 200);
        Map<Long, MerchantVenueRespDTO> venueCache = new HashMap<>();
        return Results.success(buildEventDetail(event, skuGroupByEvent, venueCache));
    }

    private Map<Long, List<MerchantTicketSkuRespDTO>> loadSkuGroup(Long eventId, int size) {
        Result<Page<MerchantTicketSkuRespDTO>> skuResult = merchantAdminRemoteService.pageQueryTicketSkus(eventId, 1, size);
        if (skuResult.isFail() || skuResult.getData() == null || skuResult.getData().getRecords() == null) {
            return Collections.emptyMap();
        }

        return skuResult.getData().getRecords().stream()
                .collect(Collectors.groupingBy(MerchantTicketSkuRespDTO::getEventId));
    }

    private boolean isVisibleEvent(MerchantEventRespDTO event) {
        return event != null && (event.getStatus() == null || event.getStatus() >= 1);
    }

    private EventListRespDTO buildEventDetail(
            MerchantEventRespDTO event,
            Map<Long, List<MerchantTicketSkuRespDTO>> skuGroupByEvent,
            Map<Long, MerchantVenueRespDTO> venueCache
    ) {
        EventListRespDTO dto = new EventListRespDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setType(event.getEventType() != null && event.getEventType() == 0 ? "Livehouse" : "演唱会");
        dto.setCover(event.getPosterUrl());
        dto.setDate(event.getStartTime());
        dto.setPerformerName(event.getPerformerName());
        dto.setArtist(event.getPerformerName());
        dto.setTicketStage(event.getTicketStage());
        dto.setStatus(event.getStatus());
        dto.setStarted(isStarted(event.getStartTime()));
        dto.setStatusText(resolveStatusText(event));
        dto.setTags(buildTags(dto.getType(), event.getStatus()));

        List<MerchantTicketSkuRespDTO> eventSkus = skuGroupByEvent.getOrDefault(event.getId(), Collections.emptyList());
        dto.setSkus(buildSkuList(eventSkus));
        dto.setMinPrice(resolveMinPrice(eventSkus));

        fillVenueInfo(dto, event.getVenueId(), venueCache);
        return dto;
    }

    private List<String> buildTags(String type, Integer status) {
        List<String> tags = new ArrayList<>();
        tags.add(type);
        if (status != null && status == 1) {
            tags.add("即将开售");
        } else if (status != null && status == 2) {
            tags.add("热卖中");
        }
        return tags;
    }

    private List<TicketSkuRespDTO> buildSkuList(List<MerchantTicketSkuRespDTO> eventSkus) {
        List<TicketSkuRespDTO> skuList = new ArrayList<>();
        for (MerchantTicketSkuRespDTO sku : eventSkus) {
            TicketSkuRespDTO skuDto = new TicketSkuRespDTO();
            skuDto.setId(sku.getId());
            skuDto.setName(sku.getTitle());
            skuDto.setPrice(sku.getSellingPrice());
            skuDto.setStock(sku.getRemainingStock());
            skuDto.setTotal(sku.getTotalStock());
            skuDto.setStage1Stock(sku.getStage1Stock());
            skuDto.setStage2Stock(sku.getStage2Stock());
            skuList.add(skuDto);
        }
        return skuList;
    }

    private BigDecimal resolveMinPrice(List<MerchantTicketSkuRespDTO> eventSkus) {
        BigDecimal minPrice = BigDecimal.ZERO;
        for (MerchantTicketSkuRespDTO sku : eventSkus) {
            if (sku.getSellingPrice() == null) {
                continue;
            }
            if (BigDecimal.ZERO.compareTo(minPrice) == 0 || sku.getSellingPrice().compareTo(minPrice) < 0) {
                minPrice = sku.getSellingPrice();
            }
        }
        return minPrice;
    }

    private void fillVenueInfo(EventListRespDTO dto, Long venueId, Map<Long, MerchantVenueRespDTO> venueCache) {
        if (venueId == null) {
            dto.setVenue(UNKNOWN_VENUE);
            dto.setCity(UNKNOWN_CITY);
            return;
        }

        MerchantVenueRespDTO venueDTO = venueCache.computeIfAbsent(venueId, id -> {
            try {
                Result<MerchantVenueRespDTO> venueResult = merchantAdminRemoteService.getVenue(id);
                if (venueResult.isSuccess() && venueResult.getData() != null) {
                    return venueResult.getData();
                }
            } catch (Exception e) {
                log.error("[Engine] 获取场馆详情失败, venueId={}", id, e);
            }
            return null;
        });

        if (venueDTO == null) {
            dto.setVenue(UNKNOWN_VENUE);
            dto.setCity(UNKNOWN_CITY);
            return;
        }

        dto.setVenue(venueDTO.getName());
        dto.setCity(venueDTO.getCity());
    }

    private boolean isStarted(Date startTime) {
        return startTime != null && startTime.getTime() <= System.currentTimeMillis();
    }

    private String resolveStatusText(MerchantEventRespDTO event) {
        String stageLabel = event.getTicketStage() != null && event.getTicketStage() == 2 ? "二开" : "一开";
        if (isStarted(event.getStartTime())) {
            return "演唱会已开演";
        }
        if (event.getStatus() == null) {
            return stageLabel + "待开售";
        }
        if (event.getStatus() == 2) {
            return stageLabel + "抢票中";
        }
        if (event.getStatus() == 1) {
            return stageLabel + "待开售";
        }
        if (event.getStatus() == 3) {
            return stageLabel + "已售罄";
        }
        return "暂未开售";
    }
}
