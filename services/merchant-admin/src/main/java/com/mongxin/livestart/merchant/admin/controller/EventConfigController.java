package com.mongxin.livestart.merchant.admin.controller;

import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dao.entity.EventConfigDO;
import com.mongxin.livestart.merchant.admin.service.EventConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 演出配置管理控制层
 * 为每场演出的“灵魂参数”（如限购数量、退票政策、选座模式）提供统一的治理入口。
 */
@RestController
@RequestMapping("/api/merchant-admin/event-config")
@RequiredArgsConstructor
public class EventConfigController {

    private final EventConfigService eventConfigService;

    /**
     * 根据演出ID查询精准配置
     *
     * @param eventId 演出关联主键ID
     * @return 演出配置详细指标
     */
    @GetMapping("/{eventId}")
    public Result<EventConfigDO> getEventConfig(@PathVariable("eventId") Long eventId) {
        return Results.success(eventConfigService.getByEventId(eventId));
    }

    /**
     * 覆盖式更新演出的核心配置
     * 支持全方位个性化定义（选座模式、实名规则、限购阈值、退票费率等），满足不同演出等级的定制需求。
     *
     * @param requestParam 配置更新请求参数（包含 eventId 基准）
     * @return 执行结果
     */
    @PutMapping("/update")
    public Result<Void> updateEventConfig(@RequestBody EventConfigDO requestParam) {
        eventConfigService.saveOrUpdateConfig(requestParam);
        return Results.success();
    }
}
