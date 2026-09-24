package com.mongxin.livestart.engine.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.entity.TicketSkuDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.base.MessageWrapper;
import com.mongxin.livestart.engine.mq.event.TicketOrderCreateEvent;
import com.mongxin.livestart.engine.mq.producer.OrderDelayCloseProducer;
import com.mongxin.livestart.engine.remote.MerchantAdminRemoteService;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuDetailRespDTO;
import com.mongxin.livestart.framework.web.Results;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class TicketOrderCreateConsumerTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private TicketSkuMapper ticketSkuMapper;
    @Mock
    private MerchantAdminRemoteService merchantAdminRemoteService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private OrderDelayCloseProducer orderDelayCloseProducer;

    @Test
    void shouldRollbackStockAndUserLimitWhenAsyncPersistFails() {
        TicketOrderCreateConsumer consumer = new TicketOrderCreateConsumer(
                orderMapper,
                orderItemMapper,
                ticketSkuMapper,
                merchantAdminRemoteService,
                stringRedisTemplate,
                transactionTemplate,
                orderDelayCloseProducer
        );

        TicketOrderCreateEvent event = TicketOrderCreateEvent.builder()
                .orderNo("O202606250001")
                .userId(1001L)
                .skuId(11L)
                .eventId(22L)
                .count(2)
                .visitorIds(List.of(1L, 2L))
                .build();
        String message = JSON.toJSONString(MessageWrapper.<TicketOrderCreateEvent>builder()
                .message(event)
                .keys(event.getOrderNo())
                .timestamp(new Date())
                .build());

        MerchantTicketSkuDetailRespDTO sku = new MerchantTicketSkuDetailRespDTO();
        sku.setId(11L);
        sku.setEventId(22L);
        sku.setSellingPrice(new BigDecimal("99.00"));
        sku.setVersion(3);
        sku.setRemainingStock(10);
        var result = Results.success(sku);

        when(merchantAdminRemoteService.getTicketSku(11L)).thenReturn(result);
        doAnswer(invocation -> {
            Consumer<SimpleTransactionStatus> callback = invocation.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(ticketSkuMapper.decrementStock(11L, 2, 3)).thenThrow(new RuntimeException("db fail"));
        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), eq("2"), eq("2")))
                .thenReturn(0L);
        consumer.onMessage(message);

        verify(stringRedisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("engine:stock:sku:11", "engine:limit:user:1001:event:22",
                        "engine:stock:rollback:create-order:O202606250001")),
                eq("2"),
                eq("2")
        );
    }

    @Test
    void shouldRetryMessageWhenRedisRollbackFails() {
        TicketOrderCreateConsumer consumer = new TicketOrderCreateConsumer(
                orderMapper, orderItemMapper, ticketSkuMapper, merchantAdminRemoteService,
                stringRedisTemplate, transactionTemplate, orderDelayCloseProducer);
        TicketOrderCreateEvent event = TicketOrderCreateEvent.builder()
                .orderNo("O202606250003").userId(1001L).skuId(11L).eventId(22L)
                .count(1).visitorIds(List.of(1L)).build();
        String message = JSON.toJSONString(MessageWrapper.<TicketOrderCreateEvent>builder()
                .message(event).keys(event.getOrderNo()).timestamp(new Date()).build());
        when(merchantAdminRemoteService.getTicketSku(11L)).thenReturn(Results.success(new MerchantTicketSkuDetailRespDTO()));
        org.mockito.Mockito.doThrow(new IllegalStateException("db fail"))
                .when(transactionTemplate).executeWithoutResult(any());
        when(stringRedisTemplate.execute(any(RedisScript.class), any(List.class), eq("1"), eq("1")))
                .thenThrow(new IllegalStateException("redis unavailable"));

        assertThrows(com.mongxin.livestart.framework.exception.ServiceException.class,
                () -> consumer.onMessage(message));
    }

    @Test
    void shouldRetryWithLatestVersionWhenFirstDbStockDecrementMisses() {
        TicketOrderCreateConsumer consumer = new TicketOrderCreateConsumer(
                orderMapper,
                orderItemMapper,
                ticketSkuMapper,
                merchantAdminRemoteService,
                stringRedisTemplate,
                transactionTemplate,
                orderDelayCloseProducer
        );

        TicketOrderCreateEvent event = TicketOrderCreateEvent.builder()
                .orderNo("O202606250002")
                .userId(1001L)
                .skuId(11L)
                .eventId(22L)
                .count(1)
                .visitorIds(List.of(1L))
                .build();
        String message = JSON.toJSONString(MessageWrapper.<TicketOrderCreateEvent>builder()
                .message(event)
                .keys(event.getOrderNo())
                .timestamp(new Date())
                .build());

        MerchantTicketSkuDetailRespDTO skuV1 = new MerchantTicketSkuDetailRespDTO();
        skuV1.setId(11L);
        skuV1.setEventId(22L);
        skuV1.setSellingPrice(new BigDecimal("99.00"));
        skuV1.setVersion(3);
        skuV1.setRemainingStock(10);

        MerchantTicketSkuDetailRespDTO skuV2 = new MerchantTicketSkuDetailRespDTO();
        skuV2.setId(11L);
        skuV2.setEventId(22L);
        skuV2.setSellingPrice(new BigDecimal("99.00"));
        skuV2.setVersion(4);
        skuV2.setRemainingStock(9);

        AtomicInteger fetchCount = new AtomicInteger();
        when(merchantAdminRemoteService.getTicketSku(11L)).thenAnswer(invocation ->
                Results.success(fetchCount.getAndIncrement() <= 1 ? skuV1 : skuV2));
        doAnswer(invocation -> {
            Consumer<SimpleTransactionStatus> callback = invocation.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(ticketSkuMapper.decrementStock(11L, 1, 3)).thenReturn(0);
        when(ticketSkuMapper.decrementStock(11L, 1, 4)).thenReturn(1);
        when(orderDelayCloseProducer.sendMessage(any())).thenReturn(buildSendResult());

        consumer.onMessage(message);

        verify(ticketSkuMapper).decrementStock(11L, 1, 3);
        verify(ticketSkuMapper).decrementStock(11L, 1, 4);
        verify(orderMapper, times(1)).insert(any(OrderDO.class));
        ArgumentCaptor<OrderItemDO> ticket = ArgumentCaptor.forClass(OrderItemDO.class);
        verify(orderItemMapper, times(1)).insert(ticket.capture());
        assertEquals(32, ticket.getValue().getCheckCode().length());
        assertEquals(1001L, Long.parseLong(ticket.getValue().getCheckCode().substring(1, 14), 36));
    }

    private SendResult buildSendResult() {
        SendResult sendResult = new SendResult();
        sendResult.setSendStatus(SendStatus.SEND_OK);
        return sendResult;
    }
}
