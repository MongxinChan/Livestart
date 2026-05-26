package com.mongxin.livestart.distribution.controller;

import com.mongxin.livestart.distribution.dto.req.TicketTaskCreateReqDTO;
import com.mongxin.livestart.distribution.service.TicketTaskService;
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
 * 门票大批量推送分发及赠送任务 Controller
 */
@Tag(name = "演出票务分销秒杀 - 大批量导入Excel推送赠票任务管理")
@RestController
@RequestMapping("/api/live-start/distribution/v1/task")
@RequiredArgsConstructor
public class TicketTaskController {

    private final TicketTaskService ticketTaskService;

    @Operation(summary = "发布大批量推送发票赠送任务", description = "支持主办方上传Excel解析出歌迷ID，并由RocketMQ消费者在分表架构下异步多线程批量赠票")
    @PostMapping
    public Result<Void> createTicketTask(@Valid @RequestBody TicketTaskCreateReqDTO requestParam) {
        ticketTaskService.createTicketTask(requestParam);
        return Results.success();
    }
}
