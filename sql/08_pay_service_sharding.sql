-- LiveStart pay-service 分库分表初始化脚本。
-- 执行前确认当前账号具备 CREATE DATABASE / CREATE TABLE / CREATE PROCEDURE 权限。

CREATE DATABASE IF NOT EXISTS `live_start_pay_0` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `live_start_pay_1` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `live_start_pay_0`;

CREATE TABLE IF NOT EXISTS `t_pay_template` (
  `id` bigint NOT NULL,
  `pay_sn` varchar(64) NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `channel` tinyint NOT NULL DEFAULT 1,
  `trade_type` tinyint NOT NULL DEFAULT 1,
  `subject` varchar(256) NOT NULL,
  `trade_no` varchar(128) DEFAULT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `pay_amount` decimal(10,2) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `gmt_payment` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_sn` (`pay_sn`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  UNIQUE KEY `uk_trade_no` (`trade_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_refund_template` (
  `id` bigint NOT NULL,
  `refund_no` varchar(64) NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `pay_sn` varchar(64) NOT NULL,
  `trade_no` varchar(128) NOT NULL,
  `refund_trade_no` varchar(128) DEFAULT NULL,
  `refund_amount` decimal(10,2) NOT NULL,
  `reason` varchar(256) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  UNIQUE KEY `uk_refund_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `t_pay_outbox` (
  `id` bigint NOT NULL,
  `event_id` varchar(64) NOT NULL,
  `aggregate_id` varchar(64) NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `payload` text NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0,
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_outbox_event_id` (`event_id`),
  KEY `idx_pay_outbox_pending` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS `sp_create_pay_shards`;
DELIMITER $$
CREATE PROCEDURE `sp_create_pay_shards`()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 16 DO
    SET @pay_sql = CONCAT('CREATE TABLE IF NOT EXISTS `t_pay_', i, '` LIKE `t_pay_template`');
    PREPARE pay_stmt FROM @pay_sql;
    EXECUTE pay_stmt;
    DEALLOCATE PREPARE pay_stmt;
    SET @refund_sql = CONCAT('CREATE TABLE IF NOT EXISTS `t_refund_', i, '` LIKE `t_refund_template`');
    PREPARE refund_stmt FROM @refund_sql;
    EXECUTE refund_stmt;
    DEALLOCATE PREPARE refund_stmt;
    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;
CALL `sp_create_pay_shards`();
DROP PROCEDURE `sp_create_pay_shards`;

USE `live_start_pay_1`;
CREATE TABLE IF NOT EXISTS `t_pay_template` LIKE `live_start_pay_0`.`t_pay_template`;
CREATE TABLE IF NOT EXISTS `t_refund_template` LIKE `live_start_pay_0`.`t_refund_template`;

DROP PROCEDURE IF EXISTS `sp_create_pay_shards`;
DELIMITER $$
CREATE PROCEDURE `sp_create_pay_shards`()
BEGIN
  DECLARE i INT DEFAULT 0;
  WHILE i < 16 DO
    SET @pay_sql = CONCAT('CREATE TABLE IF NOT EXISTS `t_pay_', i, '` LIKE `t_pay_template`');
    PREPARE pay_stmt FROM @pay_sql;
    EXECUTE pay_stmt;
    DEALLOCATE PREPARE pay_stmt;
    SET @refund_sql = CONCAT('CREATE TABLE IF NOT EXISTS `t_refund_', i, '` LIKE `t_refund_template`');
    PREPARE refund_stmt FROM @refund_sql;
    EXECUTE refund_stmt;
    DEALLOCATE PREPARE refund_stmt;
    SET i = i + 1;
  END WHILE;
END$$
DELIMITER ;
CALL `sp_create_pay_shards`();
DROP PROCEDURE `sp_create_pay_shards`;

-- 启动参数：--spring.profiles.active=pay-sharding
