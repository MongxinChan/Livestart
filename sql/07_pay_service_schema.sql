CREATE DATABASE IF NOT EXISTS `live_start_pay` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `live_start_pay`;

CREATE TABLE IF NOT EXISTS `t_pay` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiveStart支付单';
