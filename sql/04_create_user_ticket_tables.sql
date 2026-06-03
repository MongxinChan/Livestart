USE `ds_order_0`;

CREATE TABLE IF NOT EXISTS `t_user_ticket_0` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `ticket_sku_id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `status` int NOT NULL DEFAULT '0',
  `check_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `artist_promo_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_check_code` (`check_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_user_ticket_1` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_2` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_3` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_4` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_5` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_6` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_7` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_8` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_9` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_10` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_11` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_12` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_13` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_14` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_15` LIKE `t_user_ticket_0`;

USE `ds_order_1`;

CREATE TABLE IF NOT EXISTS `t_user_ticket_0` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `ticket_sku_id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `status` int NOT NULL DEFAULT '0',
  `check_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `artist_promo_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_check_code` (`check_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `t_user_ticket_1` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_2` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_3` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_4` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_5` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_6` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_7` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_8` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_9` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_10` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_11` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_12` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_13` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_14` LIKE `t_user_ticket_0`;
CREATE TABLE IF NOT EXISTS `t_user_ticket_15` LIKE `t_user_ticket_0`;
