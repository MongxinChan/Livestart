package com.mongxin.livestart.engine.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.mongxin.livestart.engine.common.biz.user.UserContext;
import com.mongxin.livestart.engine.common.biz.user.UserInfoDTO;
import com.mongxin.livestart.engine.config.AlipayConfig;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.entity.TicketSkuDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.mq.producer.OrderPaySuccessProducer;
import com.mongxin.livestart.engine.mq.producer.TicketOrderCreateProducer;
import com.mongxin.livestart.engine.remote.MerchantAdminRemoteService;
import com.mongxin.livestart.engine.remote.PayRemoteService;
import com.mongxin.livestart.engine.remote.dto.MerchantTicketSkuDetailRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantEventRespDTO;
import com.mongxin.livestart.engine.remote.dto.MerchantVenueRespDTO;
import com.mongxin.livestart.engine.remote.dto.RefundCreateResponseDTO;
import com.mongxin.livestart.engine.dto.resp.TicketVerifyStatsRespDTO;
import com.mongxin.livestart.engine.service.impl.TicketOrderServiceImpl;
import com.mongxin.livestart.engine.service.StockRestoreService;
import com.mongxin.livestart.engine.toolkit.TicketCheckCodeUtil;
import com.mongxin.livestart.framework.exception.ClientException;
import com.mongxin.livestart.framework.result.Result;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private PayRemoteService payRemoteService;
    @Mock
    private StockRestoreService stockRestoreService;
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
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, OrderDO.class);
        TableInfoHelper.initTableInfo(assistant, OrderItemDO.class);
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
        SendResult sendResult = org.mockito.Mockito.mock(SendResult.class);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderPaySuccessProducer.sendMessage(any())).thenReturn(sendResult);
        when(sendResult.getSendStatus()).thenReturn(org.apache.rocketmq.client.producer.SendStatus.SEND_OK);

        assertDoesNotThrow(() -> ticketOrderService.paySuccess("O202406100002", "TRADE-2", new BigDecimal("88.00")));
        verify(orderMapper, never()).updateOrderStatus(anyLong(), anyLong(), anyInt(), anyInt());
        verify(orderPaySuccessProducer).sendMessage(any());
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
    void shouldRejectVerifyStatsForOrdinaryUser() {
        UserContext.setUser(UserInfoDTO.builder().userId("1001").userType(1).build());

        ClientException ex = assertThrows(ClientException.class,
                () -> ticketOrderService.getVerifyStats(null));

        assertEquals("当前用户无验票统计查看权限", ex.getMessage());
        verify(orderItemMapper, never()).countPaidTickets(any(), anyBoolean());
    }

    @Test
    void shouldReturnAllVerifyStatsForSuperAdmin() {
        UserContext.setUser(UserInfoDTO.builder().userId("1").userType(4).build());
        when(orderItemMapper.countPaidTickets(null, false)).thenReturn(10L);
        when(orderItemMapper.countPaidTickets(null, true)).thenReturn(4L);

        TicketVerifyStatsRespDTO result = ticketOrderService.getVerifyStats(null);

        assertEquals(10L, result.getTotalCount());
        assertEquals(4L, result.getCheckedCount());
        assertEquals(6L, result.getUncheckedCount());
        assertEquals(new BigDecimal("40.00"), result.getCheckedRate());
        verify(merchantAdminRemoteService, never()).pageQueryEvents(anyInt(), anyInt());
    }

    @Test
    void shouldLimitVenueAdminVerifyStatsToManagedEvents() {
        UserContext.setUser(UserInfoDTO.builder().userId("7").userType(3).build());
        MerchantEventRespDTO managedEvent = event(101L, 11L);
        MerchantEventRespDTO otherEvent = event(202L, 22L);
        Page<MerchantEventRespDTO> eventPage = new Page<>(1, 200, 2);
        eventPage.setRecords(List.of(managedEvent, otherEvent));
        when(merchantAdminRemoteService.pageQueryEvents(1, 200))
                .thenReturn(new Result<Page<MerchantEventRespDTO>>()
                        .setCode(Result.SUCCESS_CODE)
                        .setData(eventPage));
        when(merchantAdminRemoteService.getVenue(11L))
                .thenReturn(successVenue(11L, 7L));
        when(merchantAdminRemoteService.getVenue(22L))
                .thenReturn(successVenue(22L, 8L));
        when(orderItemMapper.countPaidTickets(List.of(101L), false)).thenReturn(8L);
        when(orderItemMapper.countPaidTickets(List.of(101L), true)).thenReturn(3L);

        TicketVerifyStatsRespDTO result = ticketOrderService.getVerifyStats(null);

        assertEquals(8L, result.getTotalCount());
        assertEquals(3L, result.getCheckedCount());
        verify(merchantAdminRemoteService).getVenue(11L);
        verify(merchantAdminRemoteService).getVenue(22L);
    }

    @Test
    void shouldReturnEmptyVerifyStatsWhenVenueAdminHasNoManagedEvents() {
        UserContext.setUser(UserInfoDTO.builder().userId("7").userType(3).build());
        Page<MerchantEventRespDTO> eventPage = new Page<>(1, 200, 1);
        eventPage.setRecords(List.of(event(202L, 22L)));
        when(merchantAdminRemoteService.pageQueryEvents(1, 200))
                .thenReturn(new Result<Page<MerchantEventRespDTO>>()
                        .setCode(Result.SUCCESS_CODE)
                        .setData(eventPage));
        when(merchantAdminRemoteService.getVenue(22L))
                .thenReturn(successVenue(22L, 8L));

        TicketVerifyStatsRespDTO result = ticketOrderService.getVerifyStats(null);

        assertEquals(0L, result.getTotalCount());
        assertEquals(BigDecimal.ZERO, result.getCheckedRate());
        verify(orderItemMapper, never()).countPaidTickets(any(), anyBoolean());
    }

    @Test
    void shouldRejectTicketVerificationOutsideManagedVenue() {
        UserContext.setUser(UserInfoDTO.builder().userId("7").userType(3).build());
        OrderItemDO item = OrderItemDO.builder()
                .userId(20L).orderNo("ORDER-1").eventId(101L).checkCode("CODE-1").build();
        when(orderItemMapper.selectOne(any())).thenReturn(item);
        when(merchantAdminRemoteService.getEvent(101L)).thenReturn(successEvent(101L, 11L));
        when(merchantAdminRemoteService.getVenue(11L)).thenReturn(successVenue(11L, 8L));

        ClientException ex = assertThrows(ClientException.class, () -> ticketOrderService.verifyTicket("CODE-1"));

        assertEquals("无权核验其他场馆的电子票", ex.getMessage());
        verify(orderMapper, never()).selectOne(any());
        verify(orderItemMapper, never()).update(any(), any());
    }

    @Test
    void shouldVerifyTicketInsideManagedVenue() {
        UserContext.setUser(UserInfoDTO.builder().userId("7").userType(3).build());
        OrderItemDO item = OrderItemDO.builder()
                .userId(20L).orderNo("ORDER-1").eventId(101L).checkCode("CODE-1").build();
        when(orderItemMapper.selectOne(any())).thenReturn(item);
        when(merchantAdminRemoteService.getEvent(101L)).thenReturn(successEvent(101L, 11L));
        when(merchantAdminRemoteService.getVenue(11L)).thenReturn(successVenue(11L, 7L));
        when(orderMapper.selectOne(any())).thenReturn(OrderDO.builder().status(1).build());
        when(orderItemMapper.update(isNull(), any())).thenReturn(1);

        assertEquals("CODE-1", ticketOrderService.verifyTicket("CODE-1").getCheckCode());
        verify(orderItemMapper).selectOne(org.mockito.ArgumentMatchers.argThat(query ->
                !query.getSqlSegment().contains("user_id")));
        verify(orderItemMapper).update(isNull(), org.mockito.ArgumentMatchers.argThat(update ->
                update.getSqlSet().contains("checked_at")
                        && update.getSqlSet().contains("checked_by")
                        && update.getSqlSet().contains("is_checked")));
    }

    @Test
    void shouldVerifyLegacyTicketWithNullCheckedFlag() {
        UserContext.setUser(UserInfoDTO.builder().userId("1").userType(4).build());
        OrderItemDO item = OrderItemDO.builder()
                .userId(20L).orderNo("ORDER-LEGACY").eventId(101L).checkCode("LEGACY-CODE")
                .isChecked(null)
                .build();
        when(orderItemMapper.selectOne(any())).thenReturn(item);
        when(orderMapper.selectOne(any())).thenReturn(OrderDO.builder().status(1).build());
        when(orderItemMapper.update(isNull(), any())).thenReturn(1);

        assertEquals("LEGACY-CODE", ticketOrderService.verifyTicket("LEGACY-CODE").getCheckCode());
        verify(orderItemMapper).update(isNull(), org.mockito.ArgumentMatchers.argThat(update ->
                update.getSqlSegment().contains("is_checked")
                        && update.getSqlSegment().contains("IS NULL")));
    }

    @Test
    void shouldRouteNewTicketCodeAndOrderByUserId() {
        UserContext.setUser(UserInfoDTO.builder().userId("1").userType(4).build());
        String checkCode = TicketCheckCodeUtil.generate(20L);
        assertEquals(32, checkCode.length());
        OrderItemDO item = OrderItemDO.builder()
                .userId(20L).orderNo("ORDER-2").eventId(101L).checkCode(checkCode).build();
        when(orderItemMapper.selectOne(any())).thenReturn(item);
        when(orderMapper.selectOne(any())).thenReturn(OrderDO.builder().status(1).build());
        when(orderItemMapper.update(isNull(), any())).thenReturn(1);

        assertEquals(checkCode, ticketOrderService.verifyTicket(checkCode).getCheckCode());
        verify(orderItemMapper).selectOne(org.mockito.ArgumentMatchers.argThat(query ->
                query.getSqlSegment().contains("user_id")));
        verify(orderMapper).selectOne(org.mockito.ArgumentMatchers.argThat(query ->
                query.getSqlSegment().contains("user_id")));
    }

    @Test
    void shouldReturnPersistentRecentVerifyRecords() {
        UserContext.setUser(UserInfoDTO.builder().userId("1").userType(4).build());
        OrderItemDO item = OrderItemDO.builder()
                .id(11L).orderNo("ORDER-1").checkCode("CODE-1")
                .eventId(101L).isChecked(1).checkedBy(7L).checkedAt(new java.util.Date())
                .build();
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        var records = ticketOrderService.getRecentVerifyRecords(101L);

        assertEquals(1, records.size());
        assertEquals(7L, records.get(0).getCheckedBy());
        verify(orderItemMapper).selectList(org.mockito.ArgumentMatchers.argThat(query ->
                query.getSqlSegment().contains("event_id")
                        && query.getSqlSegment().contains("checked_at")));
    }

    @Test
    void shouldAllowPathTokenAfterStockIsIncreased() {
        UserContext.setUser(UserInfoDTO.builder().userId("7").userType(1).build());
        ReflectionTestUtils.setField(ticketOrderService, "autoWarmStockOnMiss", false);
        MerchantTicketSkuDetailRespDTO soldOut = new MerchantTicketSkuDetailRespDTO();
        soldOut.setId(99L);
        soldOut.setRemainingStock(0);
        MerchantTicketSkuDetailRespDTO restocked = new MerchantTicketSkuDetailRespDTO();
        restocked.setId(99L);
        restocked.setRemainingStock(5);
        when(merchantAdminRemoteService.getTicketSku(99L)).thenReturn(
                new Result<MerchantTicketSkuDetailRespDTO>().setCode(Result.SUCCESS_CODE).setData(soldOut),
                new Result<MerchantTicketSkuDetailRespDTO>().setCode(Result.SUCCESS_CODE).setData(restocked));
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        assertThrows(ClientException.class, () -> ticketOrderService.generatePathToken(99L));
        assertDoesNotThrow(() -> ticketOrderService.generatePathToken(99L));
    }

    @Test
    void shouldUseReliableCompensationWhenUserCancelsOrder() {
        UserContext.setUser(UserInfoDTO.builder().userId("5006").build());
        OrderDO order = OrderDO.builder()
                .id(6L).userId(5006L).orderNo("O202406100006").status(0).build();
        OrderItemDO item = OrderItemDO.builder()
                .orderNo(order.getOrderNo()).userId(5006L).eventId(66L).skuId(666L).build();
        com.mongxin.livestart.engine.dto.req.TicketOrderCancelReqDTO request =
                new com.mongxin.livestart.engine.dto.req.TicketOrderCancelReqDTO();
        request.setOrderNo(order.getOrderNo());

        when(orderMapper.selectOne(any())).thenReturn(order);
        when(orderItemMapper.selectList(any())).thenReturn(java.util.List.of(item));
        when(stockRestoreService.closeTimeoutOrder(6L, 5006L, order.getOrderNo(), 66L, 666L, 1))
                .thenReturn(true);

        ticketOrderService.cancelOrder(request);

        verify(stockRestoreService).closeTimeoutOrder(6L, 5006L, order.getOrderNo(), 66L, 666L, 1);
        verify(orderMapper, never()).updateOrderStatus(anyLong(), anyLong(), anyInt(), anyInt());
    }

    @Test
    void shouldRefundWhenPaymentArrivesAfterOrderWasCancelled() {
        OrderDO order = OrderDO.builder()
                .id(7L).userId(7007L).orderNo("O202406100007").status(2)
                .totalAmount(new BigDecimal("88.00")).build();
        RefundCreateResponseDTO refund = new RefundCreateResponseDTO();
        refund.setStatus(1);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(payRemoteService.refund(any(), eq("7007"), any()))
                .thenReturn(new Result<RefundCreateResponseDTO>().setCode(Result.SUCCESS_CODE).setData(refund));

        ticketOrderService.paySuccess(order.getOrderNo(), "TRADE-7", new BigDecimal("88.00"));

        verify(payRemoteService).refund(any(), eq("7007"), any());
        verify(orderMapper, never()).updateOrderStatus(anyLong(), anyLong(), anyInt(), anyInt());
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

    private MerchantEventRespDTO event(Long eventId, Long venueId) {
        MerchantEventRespDTO event = new MerchantEventRespDTO();
        event.setId(eventId);
        event.setVenueId(venueId);
        return event;
    }

    private Result<MerchantEventRespDTO> successEvent(Long eventId, Long venueId) {
        return new Result<MerchantEventRespDTO>()
                .setCode(Result.SUCCESS_CODE)
                .setData(event(eventId, venueId));
    }

    private Result<MerchantVenueRespDTO> successVenue(Long venueId, Long ownerUserId) {
        MerchantVenueRespDTO venue = new MerchantVenueRespDTO();
        venue.setId(venueId);
        venue.setOwnerUserId(ownerUserId);
        return new Result<MerchantVenueRespDTO>()
                .setCode(Result.SUCCESS_CODE)
                .setData(venue);
    }
}
