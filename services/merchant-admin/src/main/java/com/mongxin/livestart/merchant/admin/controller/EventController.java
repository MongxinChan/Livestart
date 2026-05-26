package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.framework.idempotent.NoDuplicateSubmit;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dto.req.EventPageQueryReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventSaveReqDTO;
import com.mongxin.livestart.merchant.admin.dto.req.EventUpdateReqDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventPageQueryRespDTO;
import com.mongxin.livestart.merchant.admin.dto.resp.EventQueryRespDTO;
import com.mongxin.livestart.merchant.admin.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 演出管理控制层
 */
@RestController
@RequestMapping("/api/merchant-admin/event")
@RequiredArgsConstructor
@Tag(name = "演出管理")
public class EventController {

    private final EventService eventService;

    @Operation(summary = "创建演出")
    @NoDuplicateSubmit(message = "请勿短时间内重复创建演出")
    @PostMapping("/create")
    public Result<Void> createEvent(@RequestBody EventSaveReqDTO requestParam) {
        eventService.createEvent(requestParam);
        return Results.success();
    }

    @Operation(summary = "分页查询演出列表")
    @GetMapping("/page")
    public Result<IPage<EventPageQueryRespDTO>> pageQueryEvents(EventPageQueryReqDTO requestParam) {
        return Results.success(eventService.pageQueryEvents(requestParam));
    }

    @Operation(summary = "查询演出详情")
    @GetMapping("/{id}")
    public Result<EventQueryRespDTO> getEvent(@PathVariable("id") Long id) {
        return Results.success(eventService.getEventById(id));
    }

    @Operation(summary = "修改演出信息")
    @NoDuplicateSubmit(message = "请勿短时间内重复修改演出信息")
    @PutMapping("/update")
    public Result<Void> updateEvent(@RequestBody EventUpdateReqDTO requestParam) {
        eventService.updateEvent(requestParam);
        return Results.success();
    }

    @Operation(summary = "删除演出")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteEvent(@PathVariable("id") Long id) {
        eventService.deleteEvent(id);
        return Results.success();
    }

    @Operation(summary = "演出上架开售（预售→在售）")
    @PostMapping("/publish/{id}")
    public Result<Void> publishEvent(@PathVariable("id") Long id) {
        eventService.publishEvent(id);
        return Results.success();
    }

    @Operation(summary = "演出下架（在售→下架）")
    @PostMapping("/shelve/{id}")
    public Result<Void> shelveEvent(@PathVariable("id") Long id) {
        eventService.shelveEvent(id);
        return Results.success();
    }

    @Operation(summary = "终止演出售票（不可逆）")
    @PostMapping("/terminate/{id}")
    public Result<Void> terminateEvent(@PathVariable("id") Long id) {
        eventService.terminateEvent(id);
        return Results.success();
    }
}
