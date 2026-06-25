package com.mongxin.livestart.engine.service;

import com.mongxin.livestart.engine.common.biz.user.UserContext;
import com.mongxin.livestart.engine.config.AlipayConfig;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.producer.OrderPaySuccessProducer;
import com.mongxin.livestart.engine.mq.producer.TicketOrderCreateProducer;
import com.mongxin.livestart.engine.service.impl.TicketOrderServiceImpl;
import com.mongxin.livestart.framework.exception.ClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketOrderServiceImplTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private TicketSkuMapper ticketSkuMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private OrderPaySuccessProducer orderPaySuccessProducer;
    @Mock
    private TicketOrderCreateProducer ticketOrderCreateProducer;
    @Mock
    private AlipayConfig alipayConfig;

    @InjectMocks
    private TicketOrderServiceImpl ticketOrderService;

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void shouldRejectPaySuccessWhenAmountDoesNotMatch() {
        OrderDO order = OrderDO.builder()
                .id(1L)
                .userId(1001L)
                .orderNo("O202406100001")
                .status(0)
                .totalAmount(new BigDecimal("199.00"))
                .build();
        when(orderMapper.selectOne(any())).thenReturn(order);

        ClientException ex = assertThrows(ClientException.class,
                () -> ticketOrderService.paySuccess("O202406100001", "TRADE-1", new BigDecimal("99.00")));

        assertEquals("支付金额校验失败", ex.getMessage());
        verify(orderPaySuccessProducer, never()).sendMessage(any());
    }

    @Test
    void shouldIgnoreDuplicatePaidNotification() {
        OrderDO order = OrderDO.builder()
                .id(2L)
                .userId(2002L)
                .orderNo("O202406100002")
                .status(1)
                .totalAmount(new BigDecimal("88.00"))
                .build();
        when(orderMapper.selectOne(any())).thenReturn(order);

        assertDoesNotThrow(() -> ticketOrderService.paySuccess("O202406100002", "TRADE-2", new BigDecimal("88.00")));
        verify(orderMapper, never()).updateOrderStatus(anyLong(), anyLong(), anyInt(), anyInt());
        verify(orderPaySuccessProducer, never()).sendMessage(any());
    }

    @Test
    void shouldMarkOrderPaidAndSendEvent() {
        OrderDO order = OrderDO.builder()
                .id(3L)
                .userId(3003L)
                .orderNo("O202406100003")
                .status(0)
                .totalAmount(new BigDecimal("66.00"))
                .build();
        SendResult sendResult = org.mockito.Mockito.mock(SendResult.class);

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateOrderStatus(3L, 3003L, 1, 0)).thenReturn(1);
        when(orderMapper.updatePayTime(eq(3L), eq(3003L), any())).thenReturn(1);
        when(orderPaySuccessProducer.sendMessage(any())).thenReturn(sendResult);
        when(sendResult.getSendStatus()).thenReturn(org.apache.rocketmq.client.producer.SendStatus.SEND_OK);
        doAnswer(invocation -> {
            Consumer<SimpleTransactionStatus> callback = invocation.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        ticketOrderService.paySuccess("O202406100003", "TRADE-3", new BigDecimal("66.00"));

        verify(orderMapper).updateOrderStatus(3L, 3003L, 1, 0);
        verify(orderMapper).updatePayTime(eq(3L), eq(3003L), any());
        ArgumentCaptor<com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent> captor =
                ArgumentCaptor.forClass(com.mongxin.livestart.engine.mq.event.OrderPaySuccessEvent.class);
        verify(orderPaySuccessProducer).sendMessage(captor.capture());
        assertEquals("O202406100003", captor.getValue().getOrderNo());
        assertEquals("TRADE-3", captor.getValue().getTradeNo());
        assertEquals(3003L, captor.getValue().getUserId());
    }
}
