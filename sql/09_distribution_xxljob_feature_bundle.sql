/*
 * LiveStart distribution + XXL-JOB feature bundle upgrade.
 *
 * Purpose:
 *   1. Upgrade an existing local/base Livestart schema for:
 *      - delayed sale release
 *      - ticket reminder subscription
 *      - frontend ticket-stage display
 *      - XXL-JOB executor/login defaults used by this project
 *   2. Consolidate the changes previously split across:
 *      - 03_tables_xxl_job.sql
 *      - 06_distribution_ticket_release_upgrade.sql
 *      - 07_ticket_reminder_schema.sql
 *      - 08_ticket_stage_and_reminder_upgrade.sql
 *
 * Recommended usage:
 *   - Fresh database:
 *       run 01_livestart_common_schema.sql
 *       run 02_livestart_sharded_schema.sql
 *       run 03_tables_xxl_job.sql
 *       then run this script
 *   - Existing local database:
 *       run this script directly
 *
 * Notes:
 *   - This script is designed to be re-runnable on MySQL 8.x.
 *   - Base tables/databases must already exist.
 */

DELIMITER //

DROP PROCEDURE IF EXISTS `sp_apply_distribution_xxljob_feature_bundle`//
CREATE PROCEDURE `sp_apply_distribution_xxljob_feature_bundle`()
BEGIN

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_ticket_sku' AND COLUMN_NAME = 'stage1_stock'
  ) THEN
    ALTER TABLE `live_start`.`t_ticket_sku`
      ADD COLUMN `stage1_stock` int DEFAULT NULL COMMENT '一开释放库存' AFTER `total_stock`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_ticket_sku' AND COLUMN_NAME = 'stage2_stock'
  ) THEN
    ALTER TABLE `live_start`.`t_ticket_sku`
      ADD COLUMN `stage2_stock` int DEFAULT NULL COMMENT '二开释放库存' AFTER `stage1_stock`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_ticket_sku' AND COLUMN_NAME = 'stage2_released'
  ) THEN
    ALTER TABLE `live_start`.`t_ticket_sku`
      ADD COLUMN `stage2_released` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'stage2 stock released flag' AFTER `stage2_stock`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'artist_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `artist_id` bigint NULL COMMENT 'Performer or artist id' AFTER `title`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'artist_name'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `artist_name` varchar(128) NULL COMMENT 'Performer or artist name' AFTER `artist_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'event_time'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `event_time` datetime NULL COMMENT 'Distribution event time' AFTER `artist_name`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'venue_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `venue_id` bigint NULL COMMENT 'Related venue id' AFTER `event_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'sale_start_time'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `sale_start_time` datetime NULL COMMENT 'Ticket sale start time' AFTER `venue_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'event_type'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `event_type` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Event type 0:Livehouse 1:Concert' AFTER `sale_start_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'start_time'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Canonical event start time' AFTER `venue_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'xxl_job_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `xxl_job_id` int NULL COMMENT 'Bound XXL-JOB id for scheduled release' AFTER `status`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'create_time'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time' AFTER `xxl_job_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'update_time'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time' AFTER `create_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'del_flag'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT 'Delete flag 0:not deleted 1:deleted' AFTER `update_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_ticket_reminder'
  ) THEN
    CREATE TABLE `live_start`.`t_ticket_reminder` (
      `id` bigint NOT NULL COMMENT 'Primary key',
      `event_id` bigint NOT NULL COMMENT 'Event id',
      `user_id` bigint NOT NULL COMMENT 'Subscribed user id',
      `username` varchar(64) DEFAULT NULL COMMENT 'Subscribed username',
      `phone` varchar(32) DEFAULT NULL COMMENT 'Subscribed user phone',
      `event_title` varchar(255) NOT NULL COMMENT 'Event title snapshot',
      `ticket_stage` tinyint NOT NULL DEFAULT '1' COMMENT 'Ticket stage 1:first sale 2:second sale',
      `sale_start_time` datetime NOT NULL COMMENT 'Event sale start time',
      `remind_time` datetime NOT NULL COMMENT 'Reminder trigger time',
      `status` tinyint NOT NULL DEFAULT '0' COMMENT '0:pending 1:reminded 2:canceled',
      `xxl_job_id` int DEFAULT NULL COMMENT 'Bound xxl-job id',
      `reminder_message` varchar(512) DEFAULT NULL COMMENT 'Reminder content',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
      `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT 'Delete flag 0:not deleted 1:deleted',
      PRIMARY KEY (`id`),
      KEY `idx_event_user` (`event_id`, `user_id`),
      KEY `idx_user_status` (`user_id`, `status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ticket sale reminder subscription';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event_ticket_stage'
  ) THEN
    CREATE TABLE `live_start`.`t_event_ticket_stage` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `event_id` bigint NOT NULL COMMENT 'Event id',
      `ticket_stage` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Ticket stage 1:first sale 2:second sale',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_event_stage` (`event_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Event ticket stage';
  END IF;

  INSERT INTO `live_start`.`t_event_ticket_stage` (`event_id`, `ticket_stage`)
  SELECT `id`, 1
  FROM `live_start`.`t_event`
  WHERE `id` IS NOT NULL
  ON DUPLICATE KEY UPDATE `ticket_stage` = VALUES(`ticket_stage`);

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_ticket_reminder' AND INDEX_NAME = 'idx_remind_time_status'
  ) THEN
    ALTER TABLE `live_start`.`t_ticket_reminder`
      ADD KEY `idx_remind_time_status` (`remind_time`, `status`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_ticket_reminder' AND INDEX_NAME = 'idx_user_event'
  ) THEN
    ALTER TABLE `live_start`.`t_ticket_reminder`
      ADD KEY `idx_user_event` (`user_id`, `event_id`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_ticket_reminder' AND INDEX_NAME = 'idx_xxl_job_id'
  ) THEN
    ALTER TABLE `live_start`.`t_ticket_reminder`
      ADD KEY `idx_xxl_job_id` (`xxl_job_id`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND INDEX_NAME = 'idx_sale_start_time_status'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD KEY `idx_sale_start_time_status` (`sale_start_time`, `status`);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'xxl_job' AND TABLE_NAME = 'xxl_job_group'
  ) THEN
    INSERT INTO `xxl_job`.`xxl_job_group` (`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
    VALUES (1, 'livestart-distribution-executor', 'Livestart Distribution Executor', 0, NULL, NOW())
    ON DUPLICATE KEY UPDATE
      `app_name` = VALUES(`app_name`),
      `title` = VALUES(`title`),
      `address_type` = VALUES(`address_type`),
      `address_list` = VALUES(`address_list`),
      `update_time` = VALUES(`update_time`);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'xxl_job' AND TABLE_NAME = 'xxl_job_user'
  ) THEN
    INSERT INTO `xxl_job`.`xxl_job_user` (`id`, `username`, `password`, `role`, `permission`)
    VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL)
    ON DUPLICATE KEY UPDATE
      `username` = VALUES(`username`),
      `password` = VALUES(`password`),
      `role` = VALUES(`role`),
      `permission` = VALUES(`permission`);
  END IF;

END//

CALL `sp_apply_distribution_xxljob_feature_bundle`()//
DROP PROCEDURE IF EXISTS `sp_apply_distribution_xxljob_feature_bundle`//

DELIMITER ;

