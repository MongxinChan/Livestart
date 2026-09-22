package com.mongxin.livestart.engine.job;

import com.mongxin.livestart.engine.dao.entity.OrderItemDO;
import com.mongxin.livestart.engine.dao.mapper.OrderItemMapper;
import com.mongxin.livestart.engine.service.StockRestoreService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.ResultSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
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
    private OrderItemMapper orderItemMapper;
    @Mock
    private StockRestoreService stockRestoreService;
    @Test
    void shouldCloseTimeoutOrderAndRollbackStockLikeOrderService() throws Exception {
        OrderTimeoutCloseFallbackJob job = new OrderTimeoutCloseFallbackJob(
                jdbcTemplate,
                orderItemMapper,
                stockRestoreService
        );
        ReflectionTestUtils.setField(job, "fallbackEnabled", true);
        ReflectionTestUtils.setField(job, "orderCloseDelayMinutes", 15L);
        ReflectionTestUtils.setField(job, "batchSize", 100);

        Long skuId = 11L;

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
        when(orderItemMapper.selectList(any())).thenReturn(List.of(firstItem, secondItem));
        when(stockRestoreService.closeTimeoutOrder(1L, 1001L, "O202607060001", 22L, 11L, 2))
                .thenReturn(true);

        job.closeTimeoutPendingOrders();

        verify(stockRestoreService).closeTimeoutOrder(1L, 1001L, "O202607060001", 22L, 11L, 2);
    }
}
