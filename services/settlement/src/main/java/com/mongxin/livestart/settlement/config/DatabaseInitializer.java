package com.mongxin.livestart.settlement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("[Settlement] Checking settlement database schema...");
        try {
            ensureSettlementTable();
            ensureSettlementTableColumns();
            ensureNotificationReadTable();
            ensureVisibilityScopeDependencies();
        } catch (Exception ex) {
            log.error("[Settlement] Failed to initialize settlement database schema", ex);
        }
    }

    private void ensureSettlementTable() {
        String checkTableSql = "SHOW TABLES LIKE 't_settlement'";
        var tables = jdbcTemplate.queryForList(checkTableSql);
        if (!tables.isEmpty()) {
            log.info("[Settlement] t_settlement already exists.");
            return;
        }

        String createTableSql = """
                CREATE TABLE `t_settlement` (
                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  `event_id` bigint NOT NULL COMMENT '演出ID',
                  `event_title` varchar(255) NOT NULL COMMENT '演出名称',
                  `total_tickets` int NOT NULL DEFAULT 0 COMMENT '总出票数',
                  `total_sales_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '总销售额',
                  `commission_rate` decimal(5, 4) NOT NULL DEFAULT 0.0500 COMMENT '平台扣点比例',
                  `commission_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '扣除佣金金额',
                  `settlement_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '商家应结金额',
                  `status` tinyint NOT NULL DEFAULT 0 COMMENT '结算状态 0:未结算 1:已结算 2:结算异常',
                  `error_message` text DEFAULT NULL COMMENT '结算异常信息',
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `idx_unique_event` (`event_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主办方演出票房结算单';
                """;
        jdbcTemplate.execute(createTableSql);
        log.info("[Settlement] t_settlement created.");
    }

    private void ensureSettlementTableColumns() {
        String dataType = jdbcTemplate.query(
                """
                        SELECT DATA_TYPE
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = DATABASE()
                          AND TABLE_NAME = 't_settlement'
                          AND COLUMN_NAME = 'error_message'
                        """,
                rs -> rs.next() ? rs.getString("DATA_TYPE") : null
        );

        if (dataType == null) {
            jdbcTemplate.execute("ALTER TABLE t_settlement ADD COLUMN error_message text DEFAULT NULL COMMENT '结算异常信息' AFTER status");
            log.info("[Settlement] Added t_settlement.error_message.");
            return;
        }

        if (!"text".equalsIgnoreCase(dataType)
                && !"mediumtext".equalsIgnoreCase(dataType)
                && !"longtext".equalsIgnoreCase(dataType)) {
            jdbcTemplate.execute("ALTER TABLE t_settlement MODIFY COLUMN error_message text DEFAULT NULL COMMENT '结算异常信息'");
            log.info("[Settlement] Upgraded t_settlement.error_message to TEXT.");
        }
    }

    private void ensureNotificationReadTable() {
        String checkTableSql = "SHOW TABLES LIKE 't_settlement_notification_read'";
        var tables = jdbcTemplate.queryForList(checkTableSql);
        if (!tables.isEmpty()) {
            log.info("[Settlement] t_settlement_notification_read already exists.");
            return;
        }

        String createTableSql = """
                CREATE TABLE `t_settlement_notification_read` (
                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  `user_id` bigint NOT NULL COMMENT '后台用户ID',
                  `notification_key` varchar(120) NOT NULL COMMENT '通知唯一键',
                  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `idx_user_notification` (`user_id`, `notification_key`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算通知已读记录';
                """;
        jdbcTemplate.execute(createTableSql);
        log.info("[Settlement] t_settlement_notification_read created.");
    }

    private void ensureVisibilityScopeDependencies() {
        if (hasRequiredColumn("t_venue", "owner_user_id")) {
            ensureIndex("t_venue", "idx_owner_user_id", "owner_user_id");
        }
        if (hasRequiredColumn("t_event", "venue_id")) {
            ensureIndex("t_event", "idx_venue_id", "venue_id");
        }
        ensureIndex("t_settlement", "idx_settlement_event_id", "event_id");
    }

    private boolean hasRequiredColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND COLUMN_NAME = ?
                """, Integer.class, tableName, columnName);
        if (count == null || count == 0) {
            log.error("[Settlement] Missing required visibility column {}.{}; venue-admin settlement scope may fail.", tableName, columnName);
            return false;
        }
        return true;
    }

    private void ensureIndex(String tableName, String indexName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(1)
                FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = ?
                  AND INDEX_NAME = ?
                """, Integer.class, tableName, indexName);
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.execute("ALTER TABLE `" + tableName + "` ADD INDEX `" + indexName + "` (`" + columnName + "`)");
        log.info("[Settlement] Added {}.{} for venue-admin visibility scope.", tableName, indexName);
    }
}
