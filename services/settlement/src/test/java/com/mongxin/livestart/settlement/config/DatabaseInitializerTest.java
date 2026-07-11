package com.mongxin.livestart.settlement.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DatabaseInitializerTest {

    @Test
    void shouldCreateSettlementTableWithTextErrorMessageForFreshDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockVisibilityScopeDependenciesReady(jdbcTemplate);
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement'"))
                .thenReturn(Collections.emptyList());
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement_notification_read'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement_notification_read")));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<String> extractor = invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(false);
            return extractor.extractData(resultSet);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class));

        new DatabaseInitializer(jdbcTemplate).run();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate, org.mockito.Mockito.atLeastOnce()).execute(sqlCaptor.capture());

        String createTableSql = sqlCaptor.getAllValues().stream()
                .filter(sql -> sql.contains("CREATE TABLE `t_settlement`"))
                .findFirst()
                .orElseThrow();
        assertTrue(createTableSql.contains("CREATE TABLE `t_settlement`"));
        assertTrue(createTableSql.contains("`error_message` text DEFAULT NULL"));
    }

    @Test
    void shouldAddTextErrorMessageWhenColumnMissingInExistingTable() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockVisibilityScopeDependenciesReady(jdbcTemplate);
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement")));
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement_notification_read'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement_notification_read")));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<String> extractor = invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(false);
            return extractor.extractData(resultSet);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class));

        new DatabaseInitializer(jdbcTemplate).run();

        verify(jdbcTemplate).execute("ALTER TABLE t_settlement ADD COLUMN error_message text DEFAULT NULL COMMENT '结算异常信息' AFTER status");
    }

    @Test
    void shouldUpgradeLegacyVarcharErrorMessageToText() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockVisibilityScopeDependenciesReady(jdbcTemplate);
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement")));
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement_notification_read'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement_notification_read")));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<String> extractor = invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("DATA_TYPE")).thenReturn("varchar");
            return extractor.extractData(resultSet);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class));

        new DatabaseInitializer(jdbcTemplate).run();

        verify(jdbcTemplate).execute("ALTER TABLE t_settlement MODIFY COLUMN error_message text DEFAULT NULL COMMENT '结算异常信息'");
    }

    @Test
    void shouldKeepExistingTextCompatibleErrorMessageColumn() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        mockVisibilityScopeDependenciesReady(jdbcTemplate);
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement")));
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement_notification_read'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement_notification_read")));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<String> extractor = invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("DATA_TYPE")).thenReturn("mediumtext");
            return extractor.extractData(resultSet);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class));

        new DatabaseInitializer(jdbcTemplate).run();

        verify(jdbcTemplate, never()).execute("ALTER TABLE t_settlement MODIFY COLUMN error_message text DEFAULT NULL COMMENT '结算异常信息'");
    }
    @Test
    void shouldCreateVisibilityScopeIndexesWhenMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement")));
        when(jdbcTemplate.queryForList("SHOW TABLES LIKE 't_settlement_notification_read'"))
                .thenReturn(List.of(Collections.singletonMap("Tables_in_live_start", "t_settlement_notification_read")));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class), any()))
                .thenReturn(1, 0, 1, 0, 0);
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ResultSetExtractor<String> extractor = invocation.getArgument(1);
            ResultSet resultSet = mock(ResultSet.class);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("DATA_TYPE")).thenReturn("text");
            return extractor.extractData(resultSet);
        }).when(jdbcTemplate).query(anyString(), any(ResultSetExtractor.class));

        new DatabaseInitializer(jdbcTemplate).run();

        verify(jdbcTemplate).execute("ALTER TABLE `t_venue` ADD INDEX `idx_owner_user_id` (`owner_user_id`)");
        verify(jdbcTemplate).execute("ALTER TABLE `t_event` ADD INDEX `idx_venue_id` (`venue_id`)");
        verify(jdbcTemplate).execute("ALTER TABLE `t_settlement` ADD INDEX `idx_settlement_event_id` (`event_id`)");
    }

    private void mockVisibilityScopeDependenciesReady(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(Integer.class), any()))
                .thenReturn(1);
    }
}
