package com.mongxin.livestart.engine.service;

import com.mongxin.livestart.engine.common.biz.user.UserContext;
import com.mongxin.livestart.engine.common.biz.user.UserInfoDTO;
import com.mongxin.livestart.engine.config.AlipayConfig;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.TicketSkuDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.producer.OrderPaySuccessProducer;
import com.mongxin.livestart.engine.mq.producer.TicketOrderCreateProducer;
import com.mongxin.livestart.engine.remote.MerchantAdminRemoteService;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuDetailRespDTO;
import com.mongxin.livestart.engine.service.impl.TicketOrderServiceImpl;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.result.Result;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
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
    private MerchantAdminRemoteService merchantAdminRemoteService;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
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

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ticketOrderService, "mqEnabled", true);
        ReflectionTestUtils.setField(ticketOrderService, "localOrderMode", false);
    }

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

    @Test
    void shouldSkipPaySuccessMqWhenMqDisabled() {
        ReflectionTestUtils.setField(ticketOrderService, "mqEnabled", false);

        OrderDO order = OrderDO.builder()
                .id(4L)
                .userId(4004L)
                .orderNo("O202406100004")
                .status(0)
                .totalAmount(new BigDecimal("168.00"))
                .build();
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderMapper.updateOrderStatus(4L, 4004L, 1, 0)).thenReturn(1);
        when(orderMapper.updatePayTime(eq(4L), eq(4004L), any())).thenReturn(1);
        doAnswer(invocation -> {
            Consumer<SimpleTransactionStatus> callback = invocation.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        ticketOrderService.paySuccess("O202406100004", "TRADE-4", new BigDecimal("168.00"));

        verify(orderMapper).updateOrderStatus(4L, 4004L, 1, 0);
        verify(orderMapper).updatePayTime(eq(4L), eq(4004L), any());
        verify(orderPaySuccessProducer, never()).sendMessage(any());
    }

    @Test
    void shouldFallbackToLocalOrderWhenTopicRouteMissing() throws MQClientException {
        UserContext.setUser(UserInfoDTO.builder()
                .userId("5005")
                .username("tester")
                .userType(1)
                .build());

        ReflectionTestUtils.setField(ticketOrderService, "autoWarmStockOnMiss", false);

        com.mongxin.livestart.engine.dto.req.TicketOrderCreateReqDTO request =
                new com.mongxin.livestart.engine.dto.req.TicketOrderCreateReqDTO();
        request.setSkuId(200326L);
        request.setCount(1);
        request.setVisitorIds(Collections.singletonList(90001L));

        MerchantTicketSkuDetailRespDTO sku = new MerchantTicketSkuDetailRespDTO();
        sku.setId(200326L);
        sku.setEventId(103033L);
        sku.setRemainingStock(200);
        sku.setLimitNum(6);
        sku.setVersion(1);
        sku.setSellingPrice(new BigDecimal("380.00"));

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("engine:pathtoken:5005:200326")).thenReturn("token-1");
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), any(), any(), any())).thenReturn(0L);
        when(merchantAdminRemoteService.getTicketSku(200326L)).thenReturn(new Result<MerchantTicketSkuDetailRespDTO>()
                .setCode(Result.SUCCESS_CODE)
                .setData(sku));
        when(ticketSkuMapper.decrementStock(200326L, 1, 1)).thenReturn(1);
        when(ticketOrderCreateProducer.sendMessage(any()))
                .thenThrow(new org.springframework.messaging.MessagingException("No route info of this topic",
                        new MQClientException(0, "No route info of this topic", null)));
        doAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        }).when(transactionTemplate).execute(any());
        doAnswer(invocation -> {
            Consumer<SimpleTransactionStatus> callback = invocation.getArgument(0);
            callback.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        String orderNo = ticketOrderService.createOrder(request, "token-1");

        org.junit.jupiter.api.Assertions.assertTrue(orderNo.length() >= 20);
        verify(ticketOrderCreateProducer).sendMessage(any());
        verify(ticketSkuMapper).decrementStock(200326L, 1, 1);
        verify(orderMapper).insert(any());
        verify(orderItemMapper).insert(any());
    }
}
