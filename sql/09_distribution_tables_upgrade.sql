/*
  Distribution module tables missing from the original common schema.
  Run after 01_livestart_common_schema.sql and 04_feature_and_job_upgrade.sql.
  Idempotent: never drops existing tables.
*/

USE `live_start`;

CREATE TABLE IF NOT EXISTS `t_event_performer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `performer_id` bigint NOT NULL COMMENT '艺人ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_event_performer` (`event_id`,`performer_id`),
  KEY `idx_performer_id` (`performer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出艺人关联表';

CREATE TABLE IF NOT EXISTS `t_invite_code` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '艺人用户ID',
  `invite_code` varchar(32) NOT NULL COMMENT '专属推广码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`invite_code`),
  UNIQUE KEY `uk_invite_user_active` (`user_id`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人推广码';

CREATE TABLE IF NOT EXISTS `t_invite_relation` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `inviter_user_id` bigint NOT NULL COMMENT '推广人用户ID',
  `invitee_user_id` bigint NOT NULL COMMENT '被推广人用户ID',
  `invite_code` varchar(32) NOT NULL COMMENT '推广码',
  `bind_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invitee_active` (`invitee_user_id`,`del_flag`),
  KEY `idx_inviter_user_id` (`inviter_user_id`),
  KEY `idx_invite_code` (`invite_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='推广关系';

CREATE TABLE IF NOT EXISTS `t_artist_commission_record` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `artist_id` bigint NOT NULL COMMENT '艺人用户ID',
  `artist_promo_code` varchar(32) DEFAULT NULL COMMENT '艺人推广码',
  `order_no` varchar(64) NOT NULL COMMENT '订单号',
  `ticket_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '票款',
  `commission_rate` decimal(5,4) NOT NULL DEFAULT '0.1000' COMMENT '分成比例',
  `commission_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '税前分成',
  `tax_rate` decimal(5,4) NOT NULL DEFAULT '0.2000' COMMENT '税费比例',
  `tax_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '税费',
  `actual_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '税后实得',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待结算 1已结算 2已取消',
  `settle_time` datetime DEFAULT NULL COMMENT '结算时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artist_order_active` (`artist_id`,`order_no`,`del_flag`),
  KEY `idx_artist_status` (`artist_id`,`status`),
  KEY `idx_order_no` (`order_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人分成记录';

CREATE TABLE IF NOT EXISTS `t_ticket_task` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `task_name` varchar(128) NOT NULL COMMENT '任务名称',
  `ticket_sku_id` bigint NOT NULL COMMENT '票档ID',
  `file_url` varchar(512) DEFAULT NULL COMMENT '名单文件地址',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0待执行 1执行中 2完成 3失败',
  `total_count` int NOT NULL DEFAULT '0',
  `success_count` int NOT NULL DEFAULT '0',
  `fail_count` int NOT NULL DEFAULT '0',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_status_create_time` (`status`,`create_time`),
  KEY `idx_ticket_sku_id` (`ticket_sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批量赠票任务';
