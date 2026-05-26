package com.mongxin.livestart.merchant.admin.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dto.req.EventConfigUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventConfigQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 演出配置管理控制层
 * <p>
 * 为每场演出的"灵魂参数"（如限购数量、退票政策、选座模式）提供统一的治理入口。
 */
@RestController
@RequestMapping("/api/merchant-admin/event-config")
@RequiredArgsConstructor
@Tag(name = "演出配置管理")
public class EventConfigController {

    private final EventConfigService eventConfigService;

    @Operation(summary = "根据演出ID查询演出配置")
    @GetMapping("/{eventId}")
    public Result<EventConfigQueryRespDTO> getEventConfig(@PathVariable("eventId") Long eventId) {
        return Results.success(eventConfigService.getConfigByEventId(eventId));
    }

    @Operation(summary = "更新演出配置")
    @PutMapping("/update")
    public Result<Void> updateEventConfig(@RequestBody EventConfigUpdateReqDTO requestParam) {
        eventConfigService.saveOrUpdateConfig(requestParam);
        return Results.success();
    }
}
