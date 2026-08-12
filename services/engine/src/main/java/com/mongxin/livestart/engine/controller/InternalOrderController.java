package com.mongxin.livestart.engine.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.framework.result.Result;
import com.mongxin.livestart.framework.web.Results;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

/**
 * 仅供支付服务查询可支付订单，不暴露给客户端。
 */
@RestController
@RequestMapping("/api/engine/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final OrderMapper orderMapper;

    @Value("${livestart.engine.internal-token:change-me}")
    private String internalToken;

    @GetMapping("/{orderNo}/payable")
    public Result<PayableOrderResponse> getPayableOrder(@PathVariable String orderNo,
                                                        @RequestParam Long userId,
                                                        HttpServletRequest request) {
        if (!internalToken.equals(request.getHeader("X-Internal-Token"))) {
            return failure("AUTH_ERROR", "内部调用认证失败");
        }
        OrderDO order = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderNo, orderNo)
                .eq(OrderDO::getUserId, userId));
        if (order == null || order.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getCode()) {
            return failure("ORDER_NOT_PAYABLE", "订单不存在或不可支付");
        }
        return Results.success(new PayableOrderResponse(order.getOrderNo(), order.getUserId(),
                order.getTotalAmount(), order.getStatus()));
    }

    public record PayableOrderResponse(String orderNo, Long userId, BigDecimal totalAmount, Integer status) {
    }

    private Result<PayableOrderResponse> failure(String code, String message) {
        return new Result<PayableOrderResponse>().setCode(code).setMessage(message);
    }
}
