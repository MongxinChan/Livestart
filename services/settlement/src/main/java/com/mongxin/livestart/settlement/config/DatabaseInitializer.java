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
        log.info("[结算服务] 开始检查并自动初始化结算相关表结构...");
        try {
            ensureSettlementTable();
            ensureSettlementTableColumns();
            ensureNotificationReadTable();
        } catch (Exception ex) {
            log.error("[结算服务] 自动初始化结算相关表失败，请检查数据库连接及权限", ex);
        }
    }

    private void ensureSettlementTable() {
        String checkTableSql = "SHOW TABLES LIKE 't_settlement'";
        var tables = jdbcTemplate.queryForList(checkTableSql);
        if (!tables.isEmpty()) {
            log.info("[结算服务] t_settlement 表已存在，无需创建。");
            return;
        }

        log.info("[结算服务] 检测到 t_settlement 表不存在，开始自动创建...");
        String createTableSql = """
                CREATE TABLE `t_settlement` (
                  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                  `event_id` bigint NOT NULL COMMENT '演出ID',
                  `event_title` varchar(255) NOT NULL COMMENT '演出名称',
                  `total_tickets` int NOT NULL DEFAULT 0 COMMENT '总出票数',
                  `total_sales_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '总销售额',
                  `commission_rate` decimal(5, 4) NOT NULL DEFAULT 0.0500 COMMENT '平台扣点比例(默认5%)',
                  `commission_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '扣除佣金金额',
                  `settlement_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '商家应结金额',
                  `status` tinyint NOT NULL DEFAULT 0 COMMENT '结算状态 0:未结算 1:已结算 2:结算异常',
                  `error_message` varchar(500) DEFAULT NULL COMMENT '结算异常信息',
                  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `idx_unique_event` (`event_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主办方演出票房结算单';
                """;
        jdbcTemplate.execute(createTableSql);
        log.info("[结算服务] t_settlement 表自动创建成功。");
    }

    private void ensureSettlementTableColumns() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_settlement' AND COLUMN_NAME = 'error_message'",
                Integer.class
        );
        if (count != null && count > 0) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE t_settlement ADD COLUMN error_message varchar(500) DEFAULT NULL COMMENT '结算异常信息' AFTER status");
        log.info("[结算服务] 已为 t_settlement 补充 error_message 字段。");
    }

    private void ensureNotificationReadTable() {
        String checkTableSql = "SHOW TABLES LIKE 't_settlement_notification_read'";
        var tables = jdbcTemplate.queryForList(checkTableSql);
        if (!tables.isEmpty()) {
            log.info("[结算服务] t_settlement_notification_read 表已存在，无需创建。");
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
        log.info("[结算服务] t_settlement_notification_read 表自动创建成功。");
    }
}
