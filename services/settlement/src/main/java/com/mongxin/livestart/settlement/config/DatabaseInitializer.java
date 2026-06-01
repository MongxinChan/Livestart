package com.mongxin.livestart.settlement.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库自动初始化组件
 * <p>
 * 在结算微服务启动时，自动在 live_start 库中创建 t_settlement 结算单表（如果不存在的话），
 * 避免用户手动去导入 SQL，保证开箱即用。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("[结算服务] 开始检查并自动初始化结算表结构...");
        try {
            String checkTableSql = "SHOW TABLES LIKE 't_settlement'";
            var tables = jdbcTemplate.queryForList(checkTableSql);
            if (tables.isEmpty()) {
                log.info("[结算服务] 检测到 t_settlement 表不存在，开始自动创建...");
                String createTableSql = """
                        CREATE TABLE `t_settlement` (
                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                          `event_id` bigint NOT NULL COMMENT '演出ID',
                          `event_title` varchar(255) NOT NULL COMMENT '演出名称',
                          `total_tickets` int NOT NULL DEFAULT 0 COMMENT '总出票数',
                          `total_sales_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '总销售额',
                          `commission_rate` decimal(5, 4) NOT NULL DEFAULT 0.0500 COMMENT '平台扣点比例 (默认 5%)',
                          `commission_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '扣除佣金金额',
                          `settlement_amount` decimal(12, 2) NOT NULL DEFAULT 0.00 COMMENT '商家应结金额',
                          `status` tinyint NOT NULL DEFAULT 0 COMMENT '结算状态 0:未结算 1:已结算 2:结算异常',
                          `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `idx_unique_event` (`event_id`)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主办方演出票房结算单';
                        """;
                jdbcTemplate.execute(createTableSql);
                log.info("[结算服务] t_settlement 表自动创建成功！");
            } else {
                log.info("[结算服务] t_settlement 表已存在，无需创建。");
            }
        } catch (Exception ex) {
            log.error("[结算服务] 自动初始化结算表失败，请检查数据库连接及权限", ex);
        }
    }
}
