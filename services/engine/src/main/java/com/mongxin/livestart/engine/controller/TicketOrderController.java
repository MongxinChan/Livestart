package com.mongxin.livestart.engine.controller;

import com.mongxin.livestart.engine.common.annotation.RateLimit;
import com.mongxin.livestart.framework.idempotent.NoDuplicateSubmit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.engine.dto.req.AdminOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCancelReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCheckReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCreateReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderRefundReqDTO;
import com.mongxin.livestart.engine.dto.resp.AdminOrderPageQueryRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderDetailRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderPageQueryRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketVerifyRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketVerifyRecordRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketVerifyStatsRespDTO;
import com.mongxin.livestart.engine.service.TicketOrderService;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 购票订单 Controller
 */
@Tag(name = "购票引擎 - 订单管理")
@Slf4j
@RestController
@RequestMapping("/api/engine/order")
@RequiredArgsConstructor
public class TicketOrderController {

    private final TicketOrderService ticketOrderService;

    /**
     * 获取动态抢票 URL Path Token
     */
    @Operation(summary = "获取动态抢票 URL Path Token", description = "在抢票前由后端动态生成带随机 Hash 盐值的 URL Token")
    @GetMapping("/token")
    public Result<String> generatePathToken(Long skuId) {
        return Results.success(ticketOrderService.generatePathToken(skuId));
    }

    /**
     * 购票下单
     */
    @Operation(summary = "购票下单", description = "用户选择票种和观演人后发起下单，返回订单流水号")
    @RateLimit(permits = 5, timeWindowMs = 1000)
    @NoDuplicateSubmit(message = "正在处理您的下单请求，请稍候")
    @PostMapping("/create/{pathToken}")
    public Result<String> createOrder(
            @PathVariable String pathToken,
            @Valid @RequestBody TicketOrderCreateReqDTO requestParam
    ) {
        return Results.success(ticketOrderService.createOrder(requestParam, pathToken));
    }

    /**
     * 发起支付宝沙箱支付
     */
    @Operation(summary = "发起支付宝沙箱支付", description = "获取渲染后的支付宝支付 HTML Form 表单")
    @GetMapping("/pay/alipay")
    public Result<String> payWithAlipay(@Parameter(description = "订单流水号", required = true) String orderNo) {
        return Results.success(ticketOrderService.payWithAlipay(orderNo));
    }

    /**
     * 取消订单
     */
    @Operation(summary = "取消订单", description = "用户主动取消待支付订单，自动归还库存")
    @PostMapping("/cancel")
    public Result<Void> cancelOrder(@Valid @RequestBody TicketOrderCancelReqDTO requestParam) {
        ticketOrderService.cancelOrder(requestParam);
        return Results.success();
    }

    /**
     * 退票申请
     */
    @Operation(summary = "退票申请", description = "用户对已支付订单发起退票，按演出退票策略处理")
    @NoDuplicateSubmit(message = "退票申请正在处理中，请勿重复提交")
    @PostMapping("/refund")
    public Result<Void> refundOrder(@Valid @RequestBody TicketOrderRefundReqDTO requestParam) {
        ticketOrderService.refundOrder(requestParam);
        return Results.success();
    }

    /**
     * 我的订单分页查询
     */
    @Operation(summary = "我的订单分页查询", description = "查询当前登录用户的订单列表，支持按状态筛选")
    @GetMapping("/page")
    public Result<IPage<TicketOrderPageQueryRespDTO>> pageQueryOrders(TicketOrderPageQueryReqDTO requestParam) {
        return Results.success(ticketOrderService.pageQueryOrders(requestParam));
    }

    @Operation(summary = "后台订单分页查询", description = "供管理后台使用，超管查看全量订单，场馆管理员查看所属场馆订单")
    @GetMapping("/admin/page")
    public Result<IPage<AdminOrderPageQueryRespDTO>> pageQueryAdminOrders(AdminOrderPageQueryReqDTO requestParam) {
        return Results.success(ticketOrderService.pageQueryAdminOrders(requestParam));
    }

    /**
     * 订单详情查询
     */
    @Operation(summary = "订单详情查询", description = "查询订单详情，含电子票核销码")
    @Parameter(name = "orderNo", description = "订单流水号", required = true)
    @GetMapping("/detail/{orderNo}")
    public Result<TicketOrderDetailRespDTO> getOrderDetail(@PathVariable String orderNo) {
        return Results.success(ticketOrderService.getOrderDetail(orderNo));
    }

    /**
     * 现场验票。
     */
    @Operation(summary = "现场验票", description = "后台验票人员输入电子票核销码并完成入场核验")
    @RateLimit(permits = 120, timeWindowMs = 1000)
    @PostMapping("/verify")
    public Result<TicketVerifyRespDTO> verifyTicket(@Valid @RequestBody TicketOrderCheckReqDTO requestParam) {
        return Results.success(ticketOrderService.verifyTicket(requestParam.getCheckCode()));
    }

    @Operation(summary = "验票统计", description = "超管查看全量统计，场馆管理员仅查看所属场馆的演出统计")
    @GetMapping("/verify/stats")
    public Result<TicketVerifyStatsRespDTO> getVerifyStats(
            @RequestParam(value = "eventId", required = false) Long eventId) {
        return Results.success(ticketOrderService.getVerifyStats(eventId));
    }

    @Operation(summary = "最近验票记录", description = "超管查看全量记录，场馆管理员仅查看所属场馆记录")
    @GetMapping("/verify/records")
    public Result<List<TicketVerifyRecordRespDTO>> getRecentVerifyRecords(
            @RequestParam(value = "eventId", required = false) Long eventId) {
        return Results.success(ticketOrderService.getRecentVerifyRecords(eventId));
    }
}
