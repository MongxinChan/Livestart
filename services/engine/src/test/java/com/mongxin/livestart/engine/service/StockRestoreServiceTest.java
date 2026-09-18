package com.mongxin.livestart.engine.service;

import com.mongxin.livestart.engine.common.enums.OrderStatusEnum;
import com.mongxin.livestart.engine.dao.entity.OrderDO;
import com.mongxin.livestart.engine.dao.entity.StockRestoreTaskDO;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.StockRestoreTaskMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockRestoreServiceTest {

    @Mock
    private StockRestoreTaskMapper taskMapper;
    @Mock
    private TicketSkuMapper ticketSkuMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private TransactionStatus transactionStatus;

    private StockRestoreService service;

    @BeforeEach
    void setUp() {
        service = new StockRestoreService(taskMapper, ticketSkuMapper, orderMapper,
                stringRedisTemplate, transactionTemplate);
        ReflectionTestUtils.setField(service, "retryBaseDelayMs", 1_000L);
    }

    @Test
    void shouldReuseOneRefundTaskForRepeatedRequests() {
        StockRestoreTaskDO existing = task(1L, 0, 0);
        when(taskMapper.selectOne(any())).thenReturn(existing);

        StockRestoreTaskDO actual = service.prepareRefund("O-1", 7L, 8L, 9L, 2);

        assertSame(existing, actual);
        verify(taskMapper, never()).insert(any());
    }

    @Test
    void shouldRestoreDatabaseAndRedisAndCompleteTask() {
        StockRestoreTaskDO task = task(1L, 0, 0);
        when(orderMapper.selectOne(any())).thenReturn(OrderDO.builder()
                .orderNo(task.getOrderNo()).userId(task.getUserId())
                .status(OrderStatusEnum.REFUNDED.getCode()).build());
        when(taskMapper.selectForUpdate(1L)).thenReturn(task);
        when(taskMapper.selectById(1L)).thenReturn(task);
        when(taskMapper.markDbRestored(1L)).thenReturn(1);
        when(taskMapper.markRedisRestored(1L)).thenReturn(1);
        when(ticketSkuMapper.returnStock(9L, 2)).thenReturn(1);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(transactionStatus);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(),
                eq("2"), eq("2"), anyString())).thenReturn(1L);

        service.process(task);

        verify(ticketSkuMapper).returnStock(9L, 2);
        verify(taskMapper).markDbRestored(1L);
        verify(taskMapper).markRedisRestored(1L);
        verify(taskMapper).markCompleted(1L);
    }

    @Test
    void shouldConfirmRefundBeforeRetryingOrderState() {
        StockRestoreTaskDO task = task(1L, 0, 0);
        task.setRefundConfirmed(0);
        when(taskMapper.markRefundConfirmed(1L)).thenReturn(1);

        org.junit.jupiter.api.Assertions.assertTrue(service.confirmRefund(task));
        verify(taskMapper).markRefundConfirmed(1L);
    }

    @Test
    void shouldScheduleRetryWhenDatabaseRestoreFails() {
        StockRestoreTaskDO task = task(1L, 0, 0);
        when(orderMapper.selectOne(any())).thenReturn(OrderDO.builder()
                .orderNo(task.getOrderNo()).userId(task.getUserId())
                .status(OrderStatusEnum.REFUNDED.getCode()).build());
        when(taskMapper.selectForUpdate(1L)).thenReturn(task);
        when(ticketSkuMapper.returnStock(9L, 2)).thenReturn(0);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(transactionStatus);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        service.process(task);

        verify(taskMapper).scheduleRetry(eq(1L), any(), anyString());
        verify(stringRedisTemplate, never()).execute(any(RedisScript.class), anyList(), any());
        verify(taskMapper, never()).markCompleted(1L);
    }

    private StockRestoreTaskDO task(Long id, int dbRestored, int redisRestored) {
        StockRestoreTaskDO task = new StockRestoreTaskDO();
        task.setId(id);
        task.setBizType("REFUND");
        task.setOrderNo("O-1");
        task.setUserId(7L);
        task.setEventId(8L);
        task.setSkuId(9L);
        task.setRestoreCount(2);
        task.setRefundConfirmed(1);
        task.setDbRestored(dbRestored);
        task.setRedisRestored(redisRestored);
        task.setStatus(0);
        task.setRetryCount(0);
        return task;
    }
}
