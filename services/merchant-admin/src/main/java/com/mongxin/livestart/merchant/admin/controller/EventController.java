package com.mongxin.livestart.merchant.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mongxin.livestart.framework.idempotent.NoDuplicateSubmit;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import com.mongxin.livestart.merchant.admin.dao.entity.EventDO;
import com.mongxin.livestart.merchant.admin.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/merchant-admin/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @NoDuplicateSubmit(message = "请勿短时间内重复创建演出")
    @PostMapping("/create")
    public Result<Void> createEvent(@RequestBody EventDO requestParam) {
        eventService.createEvent(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<EventDO>> listEvents() {
        return Results.success(eventService.listAllEvents());
    }

    /**
     * 分页查询演出列表
     *
     * @param current 当前页码（默认1）
     * @param size    每页数量（默认10）
     * @param status  演出状态筛选（可选）
     */
    @GetMapping("/page")
    public Result<IPage<EventDO>> pageQueryEvents(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer status) {
        return Results.success(eventService.pageQueryEvents(new Page<>(current, size), status));
    }

    /**
     * 根据 ID 查询演出详情
     */
    @GetMapping("/{id}")
    public Result<EventDO> getEvent(@PathVariable("id") Long id) {
        return Results.success(eventService.getEventById(id));
    }

    @NoDuplicateSubmit(message = "请勿短时间内重复修改演出信息")
    @PutMapping("/update")
    public Result<Void> updateEvent(@RequestBody EventDO requestParam) {
        eventService.updateEvent(requestParam);
        return Results.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteEvent(@PathVariable("id") Long id) {
        eventService.deleteEvent(id);
        return Results.success();
    }

    /**
     * 演出上架开售（预售 → 在售）
     */
    @PostMapping("/publish/{id}")
    public Result<Void> publishEvent(@PathVariable("id") Long id) {
        eventService.publishEvent(id);
        return Results.success();
    }

    /**
     * 演出下架（在售 → 下架）
     */
    @PostMapping("/shelve/{id}")
    public Result<Void> shelveEvent(@PathVariable("id") Long id) {
        eventService.shelveEvent(id);
        return Results.success();
    }

    /**
     * 终止演出售票（不可逆）
     */
    @PostMapping("/terminate/{id}")
    public Result<Void> terminateEvent(@PathVariable("id") Long id) {
        eventService.terminateEvent(id);
        return Results.success();
    }
}
