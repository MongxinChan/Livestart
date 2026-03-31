package com.mongxin.livestart.merchant.admin.controller;

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

    @PostMapping("/create")
    public Result<Void> createEvent(@RequestBody EventDO requestParam) {
        eventService.save(requestParam);
        return Results.success();
    }

    @GetMapping("/list")
    public Result<List<EventDO>> listEvents() {
        return Results.success(eventService.list());
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteEvent(@PathVariable("id") Long id) {
        eventService.removeById(id);
        return Results.success();
    }
}
