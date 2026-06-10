package com.mongxin.livestart.engine.controller;

import com.mongxin.livestart.engine.common.annotation.RateLimit;
import com.mongxin.livestart.framework.idempotent.NoDuplicateSubmit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.mongxin.livestart.engine.dto.req.TicketOrderCancelReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderCreateReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPageQueryReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderPayCallbackReqDTO;
import com.mongxin.livestart.engine.dto.req.TicketOrderRefundReqDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderDetailRespDTO;
import com.mongxin.livestart.engine.dto.resp.TicketOrderPageQueryRespDTO;
import com.mongxin.livestart.engine.service.TicketOrderService;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.mongxin.livestart.engine.config.AlipayConfig;
import com.alipay.api.internal.util.AlipaySignature;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private final AlipayConfig alipayConfig;

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
     * 支付回调（出票）
     */
    @Operation(summary = "支付回调", description = "支付网关回调通知，触发出票流程")
    @PostMapping("/pay-callback")
    public Result<Void> payCallback(@Valid @RequestBody TicketOrderPayCallbackReqDTO requestParam) {
        ticketOrderService.payCallback(requestParam);
        return Results.success();
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
     * 支付宝异步回调通知
     */
    @Operation(summary = "支付宝异步回调通知", description = "接收支付宝异步支付结果通知")
    @PostMapping("/pay/alipay/notify")
    public String alipayNotify(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(name, valueStr);
        }

        try {
            // 验签
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );

            if (signVerified) {
                String orderNo = params.get("out_trade_no");
                String tradeNo = params.get("trade_no");
                String tradeStatus = params.get("trade_status");
                String appId = params.get("app_id");
                String totalAmount = params.get("total_amount");

                if (!alipayConfig.getAppId().equals(appId)) {
                    return "fail";
                }
                if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                    ticketOrderService.paySuccess(orderNo, tradeNo, new BigDecimal(totalAmount));
                }
                return "success";
            } else {
                return "fail";
            }
        } catch (Exception e) {
            log.error("[支付宝回调] 处理异步通知异常", e);
            return "fail";
        }
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

    /**
     * 订单详情查询
     */
    @Operation(summary = "订单详情查询", description = "查询订单详情，含电子票核销码")
    @Parameter(name = "orderNo", description = "订单流水号", required = true)
    @GetMapping("/detail/{orderNo}")
    public Result<TicketOrderDetailRespDTO> getOrderDetail(@PathVariable String orderNo) {
        return Results.success(ticketOrderService.getOrderDetail(orderNo));
    }
}
