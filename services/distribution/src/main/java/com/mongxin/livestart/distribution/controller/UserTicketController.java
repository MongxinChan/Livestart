package com.mongxin.livestart.distribution.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.distribution.dto.req.TicketGrabReqDTO;
import com.mongxin.livestart.distribution.dto.resp.UserTicketRespDTO;
import com.mongxin.livestart.distribution.service.UserTicketService;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 歌迷电子门票秒杀抢购 Controller
 */
@Tag(name = "演出票务分销秒杀 - 歌迷秒杀抢票及门票管理")
@RestController
@RequestMapping("/api/live-start/distribution/v1/ticket")
@RequiredArgsConstructor
public class UserTicketController {

    private final UserTicketService userTicketService;

    @Operation(summary = "特权票高并发秒杀抢票", description = "抢购演出特价特权门票的核心高并发入口，Lua 脚本原子扣减防超领且秒杀直接出票")
    @PostMapping("/grab")
    public Result<Void> grabTicket(@Valid @RequestBody TicketGrabReqDTO requestParam) {
        userTicketService.grabTicket(requestParam);
        return Results.success();
    }

    @Operation(summary = "我的门票分页检索", description = "分页检索当前登录歌迷拥有的演出电子门票，内存零 Join 高性能自动匹配演出及票档属性")
    @Parameters({
            @Parameter(name = "pageNo", description = "页码", required = true, example = "1"),
            @Parameter(name = "pageSize", description = "每页大小", required = true, example = "10"),
            @Parameter(name = "status", description = "使用状态 0:未使用 1:已使用 2:已退票", required = false)
    })
    @GetMapping("/page")
    public Result<IPage<UserTicketRespDTO>> pageQueryUserTickets(
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status) {
        return Results.success(userTicketService.pageQueryUserTickets(pageNo, pageSize, status));
    }
}
