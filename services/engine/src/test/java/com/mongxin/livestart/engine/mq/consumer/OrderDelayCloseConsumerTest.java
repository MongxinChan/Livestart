package com.mongxin.livestart.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.service.StockRestoreService;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDelayCloseConsumerTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private StockRestoreService stockRestoreService;

    @Test
    void shouldRollbackStockAndUserLimitWhenClosingTimeoutOrder() {
        OrderDelayCloseConsumer consumer = new OrderDelayCloseConsumer(
                orderMapper,
                stockRestoreService
        );

        OrderDelayCloseEvent event = OrderDelayCloseEvent.builder()
                .orderNo("O202606250001")
                .userId(1001L)
                .skuId(11L)
                .eventId(22L)
                .count(2)
                .delayTime(System.currentTimeMillis())
                .build();
        String message = JSON.toJSONString(MessageWrapper.<OrderDelayCloseEvent>builder()
                .message(event)
                .keys(event.getOrderNo())
                .timestamp(new Date())
                .build());

        OrderDO order = OrderDO.builder()
                .id(1L)
                .orderNo(event.getOrderNo())
                .userId(event.getUserId())
                .status(0)
                .totalAmount(new BigDecimal("198.00"))
                .build();

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(stockRestoreService.closeTimeoutOrder(1L, 1001L, event.getOrderNo(),
                event.getEventId(), event.getSkuId(), event.getCount())).thenReturn(true);

        consumer.onMessage(message);

        verify(stockRestoreService).closeTimeoutOrder(1L, 1001L, event.getOrderNo(),
                event.getEventId(), event.getSkuId(), event.getCount());
    }

    @Test
    void shouldPropagateCompensationFailureForMessageRetry() {
        OrderDelayCloseConsumer consumer = new OrderDelayCloseConsumer(orderMapper, stockRestoreService);
        OrderDelayCloseEvent event = OrderDelayCloseEvent.builder()
                .orderNo("O202606250002")
                .userId(1001L)
                .skuId(11L)
                .eventId(22L)
                .count(1)
                .build();
        String message = JSON.toJSONString(MessageWrapper.<OrderDelayCloseEvent>builder()
                .message(event)
                .keys(event.getOrderNo())
                .timestamp(new Date())
                .build());
        when(orderMapper.selectOne(any())).thenReturn(OrderDO.builder()
                .id(2L).orderNo(event.getOrderNo()).userId(event.getUserId())
                .status(OrderStatusEnum.PENDING_PAYMENT.getCode()).build());
        when(stockRestoreService.closeTimeoutOrder(2L, event.getUserId(), event.getOrderNo(),
                event.getEventId(), event.getSkuId(), event.getCount()))
                .thenThrow(new IllegalStateException("task store unavailable"));

        assertThrows(IllegalStateException.class, () -> consumer.onMessage(message));
    }
}
