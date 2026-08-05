/*
 * LiveStart feature and job schema upgrade.
 *
 * Purpose:
 *   Consolidate all database schema updates for:
 *     - Multi-stage ticket sales and releases (t_event_sale_stage, t_event_sale_stage_sku)
 *     - Merchant-admin compatibility stage tables (t_event_ticket_stage, t_event_sale_stage_config)
 *     - Ticket reminder subscriptions (t_ticket_reminder with stages support)
 *     - Operation logs migration to Meituan's elegant bizlog format
 *     - Default XXL-JOB executor and user configurations
 *
 * Consolidates changes from the following historical SQL scripts:
 *   - 10_event_sale_stage.sql
 *   - 11_event_sale_stage_sku.sql
 *   - 12_alter_ticket_reminder_add_stage_fields.sql
 *   - 13_alter_operation_log_to_bizlog_schema.sql
 *
 * Recommended usage order:
 *   1. 01_livestart_common_schema.sql
 *   2. 02_livestart_sharded_schema.sql
 *   3. 03_tables_xxl_job.sql
 *   4. 04_feature_and_job_upgrade.sql (This script)
 *   5. 09_distribution_tables_upgrade.sql
 *   6. 05_test_seed_data.sql
 */

DELIMITER //

DROP PROCEDURE IF EXISTS `sp_apply_feature_and_job_upgrade`//
CREATE PROCEDURE `sp_apply_feature_and_job_upgrade`()
BEGIN

  -- 1. Upgrade t_ticket_sku with multi-stage stock configurations
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


  -- 2. Upgrade t_event with publishing and timing properties
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


  -- 3. Create t_ticket_reminder table (contains stage support natively)
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_ticket_reminder'
  ) THEN
    CREATE TABLE `live_start`.`t_ticket_reminder` (
      `id` bigint NOT NULL COMMENT 'Primary key',
      `event_id` bigint NOT NULL COMMENT 'Event id',
      `stage_id` bigint DEFAULT NULL COMMENT '关联开售阶段ID',
      `stage_no` tinyint DEFAULT NULL COMMENT '阶段序号',
      `stage_name` varchar(64) DEFAULT NULL COMMENT '阶段名称',
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
      KEY `idx_user_status` (`user_id`, `status`),
      KEY `idx_stage_id` (`stage_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Ticket sale reminder subscription';
  END IF;


  -- 4. Create merchant-admin compatibility stage tables
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

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event_sale_stage_config'
  ) THEN
    CREATE TABLE `live_start`.`t_event_sale_stage_config` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `event_id` bigint NOT NULL COMMENT 'Event id',
      `stage_no` tinyint NOT NULL COMMENT 'Stage number',
      `stage_name` varchar(64) NOT NULL COMMENT 'Stage name',
      `sale_start_time` datetime NOT NULL COMMENT 'Stage sale start time',
      `remark` varchar(255) DEFAULT NULL COMMENT 'Remark',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_event_stage_no` (`event_id`, `stage_no`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Event sale stage compatibility config';
  END IF;


  -- 5. Create detailed multi-stage configurations table t_event_sale_stage
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event_sale_stage'
  ) THEN
    CREATE TABLE `live_start`.`t_event_sale_stage` (
      `id` bigint NOT NULL COMMENT '阶段主键ID',
      `event_id` bigint NOT NULL COMMENT '关联演出ID',
      `stage_no` tinyint NOT NULL COMMENT '阶段序号 1:一开 2:二开 3:三开',
      `stage_name` varchar(64) NOT NULL COMMENT '阶段名称',
      `sale_start_time` datetime NOT NULL COMMENT '该阶段统一开售时间',
      `status` tinyint NOT NULL DEFAULT '0' COMMENT '阶段状态 0:待开售 1:已开售 2:已完成 3:已取消',
      `xxl_job_id` int DEFAULT NULL COMMENT '绑定的XXL-JOB任务ID',
      `remark` varchar(255) DEFAULT NULL COMMENT '备注',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标识',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_event_stage_no` (`event_id`, `stage_no`, `del_flag`),
      KEY `idx_sale_start_time_status` (`sale_start_time`, `status`),
      KEY `idx_event_id` (`event_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出开售阶段表';
  END IF;


  -- 6. Create detailed ticket releases table t_event_sale_stage_sku
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event_sale_stage_sku'
  ) THEN
    CREATE TABLE `live_start`.`t_event_sale_stage_sku` (
      `id` bigint NOT NULL COMMENT '阶段票档配置主键ID',
      `stage_id` bigint NOT NULL COMMENT '关联开售阶段ID',
      `event_id` bigint NOT NULL COMMENT '冗余演出ID，便于查询',
      `ticket_sku_id` bigint NOT NULL COMMENT '关联票档ID',
      `release_stock` int NOT NULL COMMENT '该阶段释放库存',
      `released_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否已释放 0:否 1:是',
      `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标识',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_stage_sku` (`stage_id`, `ticket_sku_id`, `del_flag`),
      KEY `idx_stage_id` (`stage_id`),
      KEY `idx_event_id` (`event_id`),
      KEY `idx_ticket_sku_id` (`ticket_sku_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出开售阶段票档释放表';
  END IF;


  -- 7. Add specific indexes for reminders and events
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


  -- 8. Idempotent Migration of operational logs to Meituan's elegant bizlog format
  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'username'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      CHANGE COLUMN `username` `operator_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人名称';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'operation'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      CHANGE COLUMN `operation` `operation_log` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作日志描述';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'method'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      CHANGE COLUMN `method` `type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型（如 Event、TicketSku）';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'params'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      CHANGE COLUMN `params` `modified_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '修改后数据(JSON)';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'ip'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      CHANGE COLUMN `ip` `operator_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人ID';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'execution_time'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      DROP COLUMN `execution_time`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'biz_no'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      ADD COLUMN `biz_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务编号' AFTER `type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'original_data'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      ADD COLUMN `original_data` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '原始数据(JSON)' AFTER `operation_log`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND INDEX_NAME = 'idx_type_biz_no'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      ADD KEY `idx_type_biz_no` (`type`, `biz_no`);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND INDEX_NAME = 'idx_create_time'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      ADD KEY `idx_create_time` (`create_time`);
  END IF;


  -- 9. Insert initial ticket stages for compatibility
  INSERT INTO `live_start`.`t_event_ticket_stage` (`event_id`, `ticket_stage`)
  SELECT `id`, 1
  FROM `live_start`.`t_event`
  WHERE `id` IS NOT NULL
  ON DUPLICATE KEY UPDATE `ticket_stage` = VALUES(`ticket_stage`);

  INSERT INTO `live_start`.`t_event_sale_stage_config` (`event_id`, `stage_no`, `stage_name`, `sale_start_time`, `remark`)
  SELECT
    e.`id`,
    1,
    'Presale Stage 1',
    CASE
      WHEN e.`status` = 1 THEN TIMESTAMP('2026-06-30 10:00:00')
      ELSE TIMESTAMP('2026-06-28 10:00:00')
    END,
    'historical presale stage backfill'
  FROM `live_start`.`t_event` e
  LEFT JOIN `live_start`.`t_event_sale_stage_config` cfg ON cfg.`event_id` = e.`id`
  WHERE e.`status` >= 1
    AND cfg.`event_id` IS NULL;


  -- 10. Configure default XXL-JOB executor group and administrative user credentials
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

CALL `sp_apply_feature_and_job_upgrade`()//
DROP PROCEDURE IF EXISTS `sp_apply_feature_and_job_upgrade`//

DELIMITER ;
