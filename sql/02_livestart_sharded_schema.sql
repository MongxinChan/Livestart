/*
 =========================================================================
 🎫 LiveStart 分布式分库分表建表脚本 (02_livestart_sharded_schema.sql)
 =========================================================================
 
 【职责定位】
 专门为系统中最核心的高并发、大写写瓶颈模块设计的一键部署物理分表脚本。
 物理库规划为 6 个分库：
   1. 用户库：ds_user_0, ds_user_1 (按 user_id % 2 分库，% 8 分表 t_user_0..7)
   2. 订单库：ds_order_0, ds_order_1 (按 user_id % 2 分库，% 16 分表 t_order_0..15 / t_order_item_0..15)
   3. 座位库：ds_seat_0, ds_seat_1 (按 event_id % 2 分库，% 8 分表 t_seat_0..7)
 
 【防跨库关联核心设计】
 - 采用 "Binding Table" 绑定表设计：t_order 和 t_order_item 共享相同的分片键（user_id），
   这保证了同一用户的订单及电子票子项百分之百会被存放在同一个物理库与分片表中，跨库 Join 开销直接降为 0。
 - 用户、订单、座位库中均冗余了常用的公共广播表副本（如 t_event, t_ticket_sku），以便底层驱动能本地高效执行关联查询。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================================
-- 1. 初始化 6 个分布式物理分库
-- =========================================================================
DROP DATABASE IF EXISTS `ds_user_0`;
CREATE DATABASE `ds_user_0` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP DATABASE IF EXISTS `ds_user_1`;
CREATE DATABASE `ds_user_1` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP DATABASE IF EXISTS `ds_order_0`;
CREATE DATABASE `ds_order_0` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP DATABASE IF EXISTS `ds_order_1`;
CREATE DATABASE `ds_order_1` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP DATABASE IF EXISTS `ds_seat_0`;
CREATE DATABASE `ds_seat_0` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
DROP DATABASE IF EXISTS `ds_seat_1`;
CREATE DATABASE `ds_seat_1` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;


-- =========================================================================
-- 2. 构建 ds_user_0 和 ds_user_1 (用户中心：按 user_id % 8 分表)
-- =========================================================================

-- 用户分片库 0
USE `ds_user_0`;

CREATE TABLE `t_user_template` (
  `id` bigint NOT NULL COMMENT '用户ID(分布式Snowflake)',
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户昵称',
  `password` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '加密存储的密码',
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号(全局路由核心索引)',
  `id_card` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '身份证号(AES加密存储)',
  `is_verified` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否实名认证 0:否 1:是',
  `real_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',
  `user_type` tinyint(1) NOT NULL DEFAULT '1' COMMENT '用户类型 1:乐迷 2:艺人 3:主办方 4:管理员',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '账号状态 1:正常 0:禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `delete_time` datetime DEFAULT NULL COMMENT '注销/删除时间',
  `del_flag` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除 0:正常 1:删除',
  PRIMARY KEY (`id`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='C端用户表主模板';

CREATE TABLE `t_user_profile_template` (
  `user_id` bigint NOT NULL COMMENT '关联 t_user.id',
  `mail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户头像地址',
  `gender` tinyint(1) DEFAULT '0' COMMENT '性别 0:保密 1:男 2:女',
  `signature` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '个性签名',
  `birthday` date DEFAULT NULL COMMENT '生日',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户社交资料模板';

CREATE TABLE `t_user_visitor_template` (
  `id` bigint NOT NULL COMMENT '观演人主键ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `real_name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '观演人真实姓名',
  `card_type` tinyint(1) NOT NULL DEFAULT '1' COMMENT '证件类型 1:身份证 2:护照 3:港澳通行证 4:台胞证',
  `card_no` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '证件号码 (AES加密存储)',
  `card_no_hash` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '证件号哈希值 (防重复录入)',
  `mobile` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '观演人手机号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_card` (`user_id`,`card_no_hash`,`del_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='常用观演人模板';

-- 冗余广播表副本，方便底层Join联查
CREATE TABLE `t_event` (
  `id` bigint NOT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` tinyint(1) NOT NULL,
  `venue_id` bigint NOT NULL,
  `start_time` datetime NOT NULL,
  `poster_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出表广播副本';

-- 物理克隆分片表 0..7
CREATE TABLE `t_user_0` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_1` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_2` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_3` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_4` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_5` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_6` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_7` SELECT * FROM `t_user_template` WHERE 1=0;

CREATE TABLE `t_user_profile_0` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_1` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_2` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_3` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_4` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_5` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_6` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_7` SELECT * FROM `t_user_profile_template` WHERE 1=0;

CREATE TABLE `t_user_visitor_0` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_1` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_2` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_3` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_4` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_5` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_6` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_7` SELECT * FROM `t_user_visitor_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_user_template`;
DROP TABLE IF EXISTS `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_visitor_template`;


-- 用户分片库 1
USE `ds_user_1`;

CREATE TABLE `t_user_template` SELECT * FROM `ds_user_0`.`t_user_0` WHERE 1=0;
CREATE TABLE `t_user_profile_template` SELECT * FROM `ds_user_0`.`t_user_profile_0` WHERE 1=0;
CREATE TABLE `t_user_visitor_template` SELECT * FROM `ds_user_0`.`t_user_visitor_0` WHERE 1=0;
CREATE TABLE `t_event` SELECT * FROM `ds_user_0`.`t_event` WHERE 1=0;

CREATE TABLE `t_user_0` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_1` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_2` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_3` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_4` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_5` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_6` SELECT * FROM `t_user_template` WHERE 1=0;
CREATE TABLE `t_user_7` SELECT * FROM `t_user_template` WHERE 1=0;

CREATE TABLE `t_user_profile_0` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_1` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_2` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_3` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_4` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_5` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_6` SELECT * FROM `t_user_profile_template` WHERE 1=0;
CREATE TABLE `t_user_profile_7` SELECT * FROM `t_user_profile_template` WHERE 1=0;

CREATE TABLE `t_user_visitor_0` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_1` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_2` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_3` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_4` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_5` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_6` SELECT * FROM `t_user_visitor_template` WHERE 1=0;
CREATE TABLE `t_user_visitor_7` SELECT * FROM `t_user_visitor_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_user_template`;
DROP TABLE IF EXISTS `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_visitor_template`;


-- =========================================================================
-- 3. 构建 ds_order_0 和 ds_order_1 (订单中心：按 user_id % 16 分表， Binding Table 共定位)
-- =========================================================================

-- 订单分片库 0
USE `ds_order_0`;

CREATE TABLE `t_order_template` (
  `id` bigint NOT NULL COMMENT '订单ID(分布式Snowflake)',
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '订单流水号',
  `user_id` bigint NOT NULL COMMENT '下单用户ID(Sharding Key)',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单实付总额',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态 0:待支付 1:已支付 2:已核销 3:已取消',
  `pay_time` datetime DEFAULT NULL COMMENT '支付完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表模板';

CREATE TABLE `t_order_item_template` (
  `id` bigint NOT NULL COMMENT '明细主键ID(分布式Snowflake)',
  `order_no` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关联订单号',
  `user_id` bigint NOT NULL COMMENT '下单用户ID(Sharding Key - 用于 Binding Table 强物理绑定)',
  `visitor_id` bigint NOT NULL COMMENT '关联实际观演人身份ID',
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `sku_id` bigint NOT NULL COMMENT '票档ID',
  `seat_id` bigint DEFAULT NULL COMMENT '关联座位ID',
  `check_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '唯一核销码',
  `is_checked` tinyint(1) DEFAULT '0' COMMENT '核销状态 0:未入场 1:已入场',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_check_code` (`check_code`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细模板';

-- 广播表副本，极速联查票价
CREATE TABLE `t_event` SELECT * FROM `ds_user_0`.`t_event` WHERE 1=0;

CREATE TABLE `t_ticket_sku` (
  `id` bigint NOT NULL COMMENT '票档主键ID',
  `event_id` bigint NOT NULL COMMENT '关联演出ID',
  `title` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_price` decimal(10,2) NOT NULL,
  `selling_price` decimal(10,2) NOT NULL,
  `total_stock` int NOT NULL,
  `stage1_stock` int DEFAULT NULL,
  `stage2_stock` int DEFAULT NULL,
  `stage2_released` tinyint(1) NOT NULL DEFAULT '0',
  `remaining_stock` int NOT NULL,
  `limit_num` int DEFAULT '6',
  `version` int DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='票档广播副本';

-- 物理克隆分片订单主表 t_order_0..15
CREATE TABLE `t_order_0` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_1` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_2` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_3` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_4` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_5` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_6` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_7` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_8` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_9` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_10` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_11` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_12` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_13` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_14` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_15` SELECT * FROM `t_order_template` WHERE 1=0;

-- 物理克隆分片明细表 t_order_item_0..15
CREATE TABLE `t_order_item_0` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_1` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_2` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_3` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_4` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_5` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_6` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_7` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_8` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_9` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_10` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_11` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_12` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_13` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_14` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_15` SELECT * FROM `t_order_item_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_order_template`;
DROP TABLE IF EXISTS `t_order_item_template`;

-- 用户票务分片表模板（按 user_id % 16 分表，与 t_order 共享分片键）
CREATE TABLE `t_user_ticket_template` (
  `id` bigint NOT NULL COMMENT '票务主键ID(分布式Snowflake)',
  `user_id` bigint NOT NULL COMMENT '用户ID(Sharding Key)',
  `ticket_sku_id` bigint NOT NULL COMMENT '关联票档ID',
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态 0:未使用 1:已核销',
  `check_code` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '唯一核销码',
  `artist_promo_code` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '艺人推广码',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `del_flag` int NOT NULL DEFAULT '0' COMMENT '逻辑删除 0:正常 1:删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_check_code` (`check_code`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户电子票模板';

-- 物理克隆分片用户票务表 t_user_ticket_0..15
CREATE TABLE `t_user_ticket_0` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_1` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_2` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_3` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_4` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_5` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_6` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_7` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_8` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_9` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_10` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_11` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_12` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_13` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_14` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_15` SELECT * FROM `t_user_ticket_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_user_ticket_template`;


-- 订单分片库 1
USE `ds_order_1`;

CREATE TABLE `t_order_template` SELECT * FROM `ds_order_0`.`t_order_0` WHERE 1=0;
CREATE TABLE `t_order_item_template` SELECT * FROM `ds_order_0`.`t_order_item_0` WHERE 1=0;
CREATE TABLE `t_event` SELECT * FROM `ds_order_0`.`t_event` WHERE 1=0;
CREATE TABLE `t_ticket_sku` SELECT * FROM `ds_order_0`.`t_ticket_sku` WHERE 1=0;

CREATE TABLE `t_order_0` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_1` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_2` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_3` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_4` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_5` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_6` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_7` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_8` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_9` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_10` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_11` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_12` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_13` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_14` SELECT * FROM `t_order_template` WHERE 1=0;
CREATE TABLE `t_order_15` SELECT * FROM `t_order_template` WHERE 1=0;

CREATE TABLE `t_order_item_0` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_1` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_2` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_3` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_4` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_5` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_6` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_7` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_8` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_9` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_10` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_11` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_12` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_13` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_14` SELECT * FROM `t_order_item_template` WHERE 1=0;
CREATE TABLE `t_order_item_15` SELECT * FROM `t_order_item_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_order_template`;
DROP TABLE IF EXISTS `t_order_item_template`;

-- 用户票务分片表（从 ds_order_0 克隆结构）
CREATE TABLE `t_user_ticket_template` SELECT * FROM `ds_order_0`.`t_user_ticket_0` WHERE 1=0;

CREATE TABLE `t_user_ticket_0` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_1` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_2` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_3` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_4` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_5` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_6` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_7` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_8` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_9` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_10` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_11` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_12` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_13` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_14` SELECT * FROM `t_user_ticket_template` WHERE 1=0;
CREATE TABLE `t_user_ticket_15` SELECT * FROM `t_user_ticket_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_user_ticket_template`;


-- =========================================================================
-- 4. 构建 ds_seat_0 和 ds_seat_1 (物理座位表：按 event_id % 8 分表)
-- =========================================================================

-- 座位分片库 0
USE `ds_seat_0`;

CREATE TABLE `t_seat_template` (
  `id` bigint NOT NULL COMMENT '物理座位主键ID(分布式Snowflake)',
  `event_id` bigint NOT NULL COMMENT '关联演出ID(Sharding Key)',
  `sku_id` bigint NOT NULL COMMENT '关联票档ID',
  `section` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '区域: A区/B区/C区/D区',
  `row_num` int NOT NULL COMMENT '排号',
  `col_num` int NOT NULL COMMENT '列号',
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态 0:可选 1:锁定中 2:已售出',
  PRIMARY KEY (`id`),
  KEY `idx_event_sku_section` (`event_id`,`sku_id`,`section`),
  KEY `idx_row_col` (`row_num`,`col_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位模板表';

CREATE TABLE `t_event` SELECT * FROM `ds_user_0`.`t_event` WHERE 1=0;
CREATE TABLE `t_ticket_sku` SELECT * FROM `ds_order_0`.`t_ticket_sku` WHERE 1=0;

-- 物理克隆分片座位表 t_seat_0..7
CREATE TABLE `t_seat_0` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_1` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_2` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_3` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_4` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_5` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_6` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_7` SELECT * FROM `t_seat_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_seat_template`;


-- 座位分片库 1
USE `ds_seat_1`;

CREATE TABLE `t_seat_template` SELECT * FROM `ds_seat_0`.`t_seat_0` WHERE 1=0;
CREATE TABLE `t_event` SELECT * FROM `ds_order_0`.`t_event` WHERE 1=0;
CREATE TABLE `t_ticket_sku` SELECT * FROM `ds_order_0`.`t_ticket_sku` WHERE 1=0;

CREATE TABLE `t_seat_0` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_1` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_2` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_3` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_4` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_5` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_6` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_7` SELECT * FROM `t_seat_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_seat_template`;

SET FOREIGN_KEY_CHECKS = 1;
