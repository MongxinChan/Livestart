package com.mongxin.livestart.distribution.controller;

import com.mongxin.livestart.distribution.dto.req.EventPublishReqDTO;
import com.mongxin.livestart.distribution.service.EventService;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 演唱会发布 Controller
 */
@Tag(name = "演出票务分销秒杀 - 演唱会发布管理")
@RestController
@RequestMapping("/api/live-start/distribution/v1/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "发布演唱会及票档", description = "主办方/平台发布演出信息并批量添加门票票档规格和发售库存")
    @PostMapping
    public Result<Void> publishEvent(@Valid @RequestBody EventPublishReqDTO requestParam) {
        eventService.publishEvent(requestParam);
        return Results.success();
    }
}
