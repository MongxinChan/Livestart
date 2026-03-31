package com.mongxin.livestart.merchant.admin.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/merchant-admin/event-config")
@RequiredArgsConstructor
public class EventConfigController {

    private final EventConfigService eventConfigService;

    /**
     * 查询演出配置（按演出ID）
     */
    @GetMapping("/{eventId}")
    public Result<EventConfigDO> getEventConfig(@PathVariable("eventId") Long eventId) {
        return Results.success(eventConfigService.getByEventId(eventId));
    }

    /**
     * 修改演出配置（selectionMode、isVerifyRequired、maxTicketsPerUser、refundPolicyType 等全字段）
     * 存在则更新，不存在则新建（幂等 upsert）
     */
    @PutMapping("/update")
    public Result<Void> updateEventConfig(@RequestBody EventConfigDO requestParam) {
        eventConfigService.saveOrUpdateConfig(requestParam);
        return Results.success();
    }
}
