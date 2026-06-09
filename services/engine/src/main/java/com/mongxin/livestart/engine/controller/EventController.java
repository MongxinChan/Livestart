package com.mongxin.livestart.engine.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 演出信息 Controller（面向 C 端）
 * <p>
 * 通过 OpenFeign 远程调用 merchant-admin 微服务获取演出和票档数据，
 * 聚合后返回给前端，遵循微服务间数据隔离原则。
 */
@Slf4j
@Tag(name = "购票引擎 - 演出信息")
@RestController
@RequestMapping("/api/engine/event")
@RequiredArgsConstructor
public class EventController {

    private final MerchantAdminRemoteService merchantAdminRemoteService;

    /**
     * C 端演出列表（含票档信息）
     * <p>
     * 聚合 merchant-admin 的演出 + 票档数据，组装为面向用户的演出信息
     */
    @Operation(summary = "演出列表", description = "通过 Feign 远程调用商户后台获取在售演出列表，含各票档库存与价格")
    @GetMapping("/list")
    public Result<List<EventListRespDTO>> listEvents() {
        log.info("[Engine] 拉取 C 端演出列表，远程调用 merchant-admin...");

        // 1. Feign 获取演出列表
        Result<IPage<MerchantEventRespDTO>> eventResult = merchantAdminRemoteService.pageQueryEvents(1, 50);
        if (eventResult.isFail() || eventResult.getData() == null) {
            log.warn("[Engine] 远程调用 merchant-admin 获取演出列表失败: {}", eventResult.getMessage());
            return Results.success(Collections.emptyList());
        }

        List<MerchantEventRespDTO> events = eventResult.getData().getRecords();
        if (events == null || events.isEmpty()) {
            return Results.success(Collections.emptyList());
        }

        // 2. Feign 获取全部票档
        Result<IPage<MerchantTicketSkuRespDTO>> skuResult = merchantAdminRemoteService.pageQueryTicketSkus(null, 1, 500);
        Map<Long, List<MerchantTicketSkuRespDTO>> skuGroupByEvent = Collections.emptyMap();
        if (skuResult.isSuccess() && skuResult.getData() != null && skuResult.getData().getRecords() != null) {
            skuGroupByEvent = skuResult.getData().getRecords().stream()
                    .collect(Collectors.groupingBy(MerchantTicketSkuRespDTO::getEventId));
        }

        // 3. 聚合组装
        List<EventListRespDTO> resultList = new ArrayList<>();
        Map<Long, MerchantVenueRespDTO> venueCache = new HashMap<>();
        for (MerchantEventRespDTO event : events) {
            // 仅返回在售和预售的演出（status = 1:预售 or 2:在售）
            if (event.getStatus() != null && event.getStatus() < 1) {
                continue;
            }

            EventListRespDTO dto = new EventListRespDTO();
            dto.setId(event.getId());
            dto.setTitle(event.getTitle());
            dto.setType(event.getEventType() != null && event.getEventType() == 0 ? "Livehouse" : "演唱会");
            dto.setCover(event.getPosterUrl());
            dto.setDate(event.getStartTime());
            dto.setPerformerName(event.getPerformerName());
            dto.setArtist(event.getPerformerName());
            dto.setTicketStage(event.getTicketStage());

            // 标签生成
            List<String> tags = new ArrayList<>();
            tags.add(dto.getType());
            if (event.getStatus() != null && event.getStatus() == 1) {
                tags.add("即将开售");
            } else if (event.getStatus() != null && event.getStatus() == 2) {
                tags.add("热卖中");
            }
            dto.setTags(tags);

            // 票档聚合
            List<MerchantTicketSkuRespDTO> eventSkus = skuGroupByEvent.getOrDefault(event.getId(), Collections.emptyList());
            BigDecimal minPrice = BigDecimal.ZERO;
            List<TicketSkuRespDTO> skuList = new ArrayList<>();
            for (MerchantTicketSkuRespDTO sku : eventSkus) {
                TicketSkuRespDTO skuDto = new TicketSkuRespDTO();
                skuDto.setId(sku.getId());
                skuDto.setName(sku.getTitle());
                skuDto.setPrice(sku.getSellingPrice());
                skuDto.setStock(sku.getRemainingStock());
                skuDto.setTotal(sku.getTotalStock());
                skuList.add(skuDto);

                if (minPrice.compareTo(BigDecimal.ZERO) == 0 ||
                        (sku.getSellingPrice() != null && sku.getSellingPrice().compareTo(minPrice) < 0)) {
                    minPrice = sku.getSellingPrice();
                }
            }
            dto.setSkus(skuList);
            dto.setMinPrice(minPrice);

            // 通过 Feign 获取场馆名和城市并利用 Map 做局部缓存防止 1+N 频繁调用
            if (event.getVenueId() != null) {
                MerchantVenueRespDTO venueDTO = venueCache.computeIfAbsent(event.getVenueId(), id -> {
                    try {
                        Result<MerchantVenueRespDTO> venueResult = merchantAdminRemoteService.getVenue(id);
                        if (venueResult.isSuccess() && venueResult.getData() != null) {
                            return venueResult.getData();
                        }
                    } catch (Exception e) {
                        log.error("[Engine] Feign 远程调用获取场馆详情失败, venueId={}", id, e);
                    }
                    return null;
                });
                if (venueDTO != null) {
                    dto.setVenue(venueDTO.getName());
                    dto.setCity(venueDTO.getCity());
                } else {
                    dto.setVenue("未知场馆");
                    dto.setCity("未知城市");
                }
            } else {
                dto.setVenue("未知场馆");
                dto.setCity("未知城市");
            }

            resultList.add(dto);
        }

        log.info("[Engine] 演出列表聚合完成，共 {} 场在售演出", resultList.size());
        return Results.success(resultList);
    }
}
