package com.mongxin.livestart.engine.job;

import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.dao.mapper.OrderMapper;
import com.mongxin.livestart.engine.dao.mapper.TicketSkuMapper;
import com.mongxin.livestart.engine.service.impl.TicketOrderServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutCloseFallbackJobTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private TicketSkuMapper ticketSkuMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void tearDown() {
        soldOutMap().clear();
    }

    @Test
    void shouldCloseTimeoutOrderAndRollbackStockLikeOrderService() throws Exception {
        OrderTimeoutCloseFallbackJob job = new OrderTimeoutCloseFallbackJob(
                jdbcTemplate,
                orderMapper,
                orderItemMapper,
                ticketSkuMapper,
                stringRedisTemplate
        );
        ReflectionTestUtils.setField(job, "fallbackEnabled", true);
        ReflectionTestUtils.setField(job, "orderCloseDelayMinutes", 15L);
        ReflectionTestUtils.setField(job, "batchSize", 100);

        Long skuId = 11L;
        soldOutMap().put(skuId, true);

        AtomicInteger queryCount = new AtomicInteger();
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            RowMapper<Object> rowMapper = invocation.getArgument(1);
            if (queryCount.getAndIncrement() > 0) {
                return List.of();
            }
            ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
            when(resultSet.getLong("id")).thenReturn(1L);
            when(resultSet.getString("order_no")).thenReturn("O202607060001");
            when(resultSet.getLong("user_id")).thenReturn(1001L);
            return List.of(rowMapper.mapRow(resultSet, 0));
        }).when(jdbcTemplate).query(
                anyString(),
                ArgumentMatchers.<RowMapper<Object>>any(),
                any(),
                any(),
                any()
        );

        OrderItemDO firstItem = OrderItemDO.builder()
                .orderNo("O202607060001")
                .userId(1001L)
                .eventId(22L)
                .skuId(skuId)
                .build();
        OrderItemDO secondItem = OrderItemDO.builder()
                .orderNo("O202607060001")
                .userId(1001L)
                .eventId(22L)
                .skuId(skuId)
                .build();
        when(orderMapper.updateOrderStatus(1L, 1001L, 2, 0)).thenReturn(1);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(firstItem, secondItem));

        job.closeTimeoutPendingOrders();

        verify(orderMapper).updateOrderStatus(1L, 1001L, 2, 0);
        verify(stringRedisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("engine:stock:sku:11", "engine:limit:user:1001:event:22")),
                eq("2"),
                eq("2")
        );
        verify(ticketSkuMapper).returnStock(11L, 2);
        assertFalse(soldOutMap().containsKey(skuId));
    }

    @SuppressWarnings("unchecked")
    private ConcurrentHashMap<Long, Boolean> soldOutMap() {
        return (ConcurrentHashMap<Long, Boolean>) ReflectionTestUtils.getField(
                TicketOrderServiceImpl.class,
                "SOLD_OUT_MAP"
        );
    }
}
