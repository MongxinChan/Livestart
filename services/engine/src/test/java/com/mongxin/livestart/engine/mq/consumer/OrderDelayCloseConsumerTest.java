package com.mongxin.livestart.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.OrderDelayCloseEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderDelayCloseConsumerTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private TicketSkuMapper ticketSkuMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void shouldRollbackStockAndUserLimitWhenClosingTimeoutOrder() {
        OrderDelayCloseConsumer consumer = new OrderDelayCloseConsumer(
                orderMapper,
                ticketSkuMapper,
                stringRedisTemplate
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
        when(orderMapper.updateOrderStatus(1L, 1001L, 2, 0)).thenReturn(1);

        consumer.onMessage(message);

        verify(stringRedisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("engine:stock:sku:11", "engine:limit:user:1001:event:22")),
                eq("2"),
                eq("2")
        );
        verify(ticketSkuMapper).returnStock(11L, 2);
    }
}
