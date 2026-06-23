package com.mongxin.livestart.distribution.controller;

import com.mongxin.livestart.distribution.dto.req.TicketReminderCreateReqDTO;
import com.mongxin.livestart.distribution.dto.resp.TicketReminderRespDTO;
import com.mongxin.livestart.distribution.service.TicketReminderService;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "演出票务分销秒杀 - 开售提醒")
@RestController
@RequestMapping("/api/live-start/distribution/v1/reminder")
@RequiredArgsConstructor
public class TicketReminderController {

    private final TicketReminderService ticketReminderService;

    @Operation(summary = "订阅演出开售提醒")
    @PostMapping("/subscribe")
    public Result<Long> subscribeReminder(@Valid @RequestBody TicketReminderCreateReqDTO requestParam) {
        return Results.success(ticketReminderService.subscribeReminder(requestParam));
    }

    @Operation(summary = "查询当前用户的开售提醒")
    @GetMapping("/list")
    public Result<List<TicketReminderRespDTO>> listCurrentUserReminders() {
        return Results.success(ticketReminderService.listCurrentUserReminders());
    }
}
