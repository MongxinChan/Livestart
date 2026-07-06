package com.mongxin.livestart.settlement.service.impl;

import com.mongxin.livestart.framework.exception.ServiceException;
import com.mongxin.livestart.settlement.common.biz.user.UserContext;
import com.mongxin.livestart.settlement.common.biz.user.UserInfoDTO;
import com.mongxin.livestart.settlement.dao.mapper.SettlementMapper;
import com.mongxin.livestart.settlement.dto.resp.SettlementShardRespDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettlementServiceImplTest {

    @AfterEach
    void tearDown() {
        UserContext.removeUser();
    }

    @Test
    void shouldTruncateErrorMessageToOneThousandChars() {
        SettlementServiceImpl service = new SettlementServiceImpl(mock(SettlementMapper.class), mock(JdbcTemplate.class));
        String message = "x".repeat(1200);

        String result = ReflectionTestUtils.invokeMethod(
                service,
                "extractErrorMessage",
                new ServiceException(message)
        );

        assertEquals(1000, result.length());
    }

    @Test
    void shouldUseDefaultErrorMessageWhenExceptionMessageIsBlank() {
        SettlementServiceImpl service = new SettlementServiceImpl(mock(SettlementMapper.class), mock(JdbcTemplate.class));

        String result = ReflectionTestUtils.invokeMethod(
                service,
                "extractErrorMessage",
                new ServiceException("")
        );

        assertEquals("结算执行失败，请联系管理员排查", result);
    }

    @Test
    void shouldScanOnlyPaidOrdersForSettlementAmount() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SettlementServiceImpl service = new SettlementServiceImpl(mock(SettlementMapper.class), jdbcTemplate);
        Map<Long, BigDecimal> skuPriceMap = Map.of(
                11L, new BigDecimal("100.00"),
                12L, new BigDecimal("50.00")
        );
        Set<Long> eventIds = Set.of(101L);

        doAnswer(invocation -> {
            RowCallbackHandler handler = invocation.getArgument(1);
            ResultSet first = mock(ResultSet.class);
            when(first.getLong("sku_id")).thenReturn(11L);
            when(first.getLong("ticket_count")).thenReturn(2L);
            handler.processRow(first);

            ResultSet second = mock(ResultSet.class);
            when(second.getLong("sku_id")).thenReturn(12L);
            when(second.getLong("ticket_count")).thenReturn(1L);
            handler.processRow(second);
            return null;
        }).when(jdbcTemplate).query(anyString(), any(RowCallbackHandler.class), any());

        SettlementShardRespDTO result = ReflectionTestUtils.invokeMethod(
                service,
                "scanSingleShard",
                "ds_order_0",
                3,
                eventIds,
                skuPriceMap
        );

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowCallbackHandler.class), any());

        String sql = sqlCaptor.getValue();
        assertTrue(sql.contains("AND o.status = 1"));
        assertFalse(sql.contains("o.status = 2"));
        assertFalse(sql.contains("o.status IN"));
        assertEquals(3, result.getTotalTickets());
        assertEquals(new BigDecimal("250.00"), result.getTotalSalesAmount());
        assertEquals(new BigDecimal("12.50"), result.getCommissionAmount());
        assertEquals(new BigDecimal("237.50"), result.getSettlementAmount());
    }

    @Test
    void shouldNotGroupByTextErrorMessageWhenPagingSettlements() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SettlementServiceImpl service = new SettlementServiceImpl(mock(SettlementMapper.class), jdbcTemplate);
        UserContext.setUser(new UserInfoDTO("1", "admin", null, null, 4));

        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Long.class), any()))
                .thenReturn(0L);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
                .thenReturn(Collections.emptyList());

        service.pageSettlements(null, "artist", "updateTime", "descend", 1, 10);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).query(sqlCaptor.capture(), any(RowMapper.class), any());

        String sql = sqlCaptor.getValue();
        assertFalse(sql.contains("GROUP BY"));
        assertTrue(sql.contains("s.error_message"));
        assertTrue(sql.contains("EXISTS"));
    }
}
