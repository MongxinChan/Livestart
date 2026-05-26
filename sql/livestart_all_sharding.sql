/*
 Livestart 分库分表一键部署终极脚本 (livestart_all_sharding.sql)
 基于高并发微服务业务架构进行重构整合
 
 主要职责：
 1. 一键执行：开发人员仅需连接本地 MySQL 运行此单一文件，即可自动初始化 7 个分物理库及全部 80+ 张分片物理表与广播表。
 2. 物理库包含：
    - ds_common (公共与账号映射库)
    - ds_user_0, ds_user_1 (用户分库，每库8表)
    - ds_order_0, ds_order_1 (订单分库，每库16表，Colocation绑定设计)
    - ds_seat_0, ds_seat_1 (座位与库存分库，每库8表)
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================================
-- 1. 初始化 7 个分布式物理库
-- =========================================================================
CREATE DATABASE IF NOT EXISTS `ds_common` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `ds_user_0` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `ds_user_1` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `ds_order_0` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `ds_order_1` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `ds_seat_0` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS `ds_seat_1` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;


-- =========================================================================
-- 2. 构建 ds_common 库 (公共配置、元数据与路由映射)
-- =========================================================================
USE `ds_common`;

DROP TABLE IF EXISTS `t_user_phone_mapping`;
CREATE TABLE `t_user_phone_mapping` (
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `user_id` bigint NOT NULL COMMENT '关联分布式用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`phone`) USING BTREE,
  UNIQUE INDEX `idx_unique_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '用户手机号与全局ID映射表';

DROP TABLE IF EXISTS `t_venue`;
CREATE TABLE `t_venue` (
  `id` bigint NOT NULL COMMENT '场馆分布式唯一ID',
  `name` varchar(255) NOT NULL COMMENT '场馆名称',
  `city` varchar(64) NOT NULL COMMENT '城市',
  `address` varchar(512) NOT NULL COMMENT '详细地址',
  `capacity` int NULL DEFAULT NULL COMMENT '容量',
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '场馆信息表';

DROP TABLE IF EXISTS `t_performer`;
CREATE TABLE `t_performer` (
  `id` bigint NOT NULL COMMENT '艺人分布式唯一ID',
  `name` varchar(128) NOT NULL COMMENT '姓名',
  `style_id` bigint NULL DEFAULT NULL,
  `avatar` varchar(512) NULL DEFAULT NULL,
  `bio` text NULL,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '艺人信息表';

DROP TABLE IF EXISTS `t_style`;
CREATE TABLE `t_style` (
  `id` bigint NOT NULL COMMENT '风格唯一ID',
  `name` varchar(64) NOT NULL,
  `code` varchar(32) NULL DEFAULT NULL,
  `description` varchar(255) NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_unique_name`(`name` ASC)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '音乐风格定义表';

DROP TABLE IF EXISTS `t_performer_style_relation`;
CREATE TABLE `t_performer_style_relation` (
  `performer_id` bigint NOT NULL,
  `style_id` bigint NOT NULL,
  PRIMARY KEY (`performer_id`, `style_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '艺人风格关联中间表';

DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment` (
  `id` bigint NOT NULL COMMENT '评论唯一ID',
  `event_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text NOT NULL,
  `status` tinyint NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_event_id`(`event_id` ASC)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '评论回复表';


-- =========================================================================
-- 3. 构建 ds_user_0 和 ds_user_1 分库与物理表
-- =========================================================================

-- 定义局部通用建表和分片函数（通过 USE 驱动，并在循环中克隆）
-- 下面分别在 ds_user_0 和 ds_user_1 中克隆 t_user_{0..7} 等物理表

-- 用户分片库 0
USE `ds_user_0`;
CREATE TABLE `t_user_template` (
  `id` bigint NOT NULL COMMENT '用户ID(Snowflake)',
  `username` varchar(64) NULL DEFAULT NULL COMMENT '昵称',
  `password` varchar(255) NULL DEFAULT NULL COMMENT '密码',
  `phone` varchar(20) NOT NULL COMMENT '手机号',
  `id_card` varchar(255) NULL DEFAULT NULL COMMENT '身份证号',
  `is_verified` tinyint(1) NOT NULL DEFAULT 0,
  `real_name` varchar(64) NULL DEFAULT NULL,
  `user_type` tinyint(1) NOT NULL DEFAULT 1,
  `status` tinyint(1) NOT NULL DEFAULT 1,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_time` datetime NULL DEFAULT NULL,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = 'C端用户表模板';

CREATE TABLE `t_user_profile_template` (
  `user_id` bigint NOT NULL COMMENT '关联 t_user.id',
  `mail` varchar(255) NULL DEFAULT NULL,
  `avatar` varchar(512) NULL DEFAULT NULL,
  `gender` tinyint(1) NULL DEFAULT 0,
  `signature` varchar(255) NULL DEFAULT NULL,
  `birthday` date NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '用户资料模板';

CREATE TABLE `t_user_visitor_template` (
  `id` bigint NOT NULL COMMENT '观演人ID(Snowflake)',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `real_name` varchar(64) NOT NULL,
  `card_type` tinyint(1) NOT NULL DEFAULT 1,
  `card_no` varchar(255) NOT NULL,
  `card_no_hash` varchar(64) NOT NULL,
  `mobile` varchar(20) NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE INDEX `idx_user_card`(`user_id` ASC, `card_no_hash` ASC, `del_flag` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '常用观演人模板';

-- 广播配置表副本
CREATE TABLE `t_event` (
  `id` bigint NOT NULL,
  `title` varchar(255) NOT NULL,
  `event_type` tinyint(1) NOT NULL,
  `venue_id` bigint NOT NULL,
  `start_time` datetime NOT NULL,
  `poster_url` varchar(512) NULL DEFAULT NULL,
  `status` tinyint(1) NULL DEFAULT 1,
  PRIMARY KEY (`id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '演出表广播副本';

CREATE TABLE `t_event_config` (
  `event_id` bigint NOT NULL,
  `selection_mode` tinyint(1) NOT NULL DEFAULT 0,
  `is_verify_required` tinyint(1) NOT NULL DEFAULT 1,
  `max_tickets_per_user` int NULL DEFAULT 6,
  `refund_policy_type` tinyint(1) NOT NULL DEFAULT 0,
  `tier1_free_refund_hours` int NULL DEFAULT NULL,
  `tier2_partial_refund_hours` int NULL DEFAULT NULL,
  `tier2_refund_fee_rate` decimal(5, 2) NULL DEFAULT 0.00,
  `is_transferable` tinyint(1) NOT NULL DEFAULT 0,
  `is_waiting_allowed` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`event_id`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '演出配置广播副本';

-- 克隆 ds_user_0 的 8 张物理表
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
-- 复制公共模板结构
CREATE TABLE `t_user_template` SELECT * FROM `ds_user_0`.`t_user_0` WHERE 1=0;
CREATE TABLE `t_user_profile_template` SELECT * FROM `ds_user_0`.`t_user_profile_0` WHERE 1=0;
CREATE TABLE `t_user_visitor_template` SELECT * FROM `ds_user_0`.`t_user_visitor_0` WHERE 1=0;
CREATE TABLE `t_event` SELECT * FROM `ds_user_0`.`t_event` WHERE 1=0;
CREATE TABLE `t_event_config` SELECT * FROM `ds_user_0`.`t_event_config` WHERE 1=0;

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
-- 4. 构建 ds_order_0 和 ds_order_1 分库与物理表
-- =========================================================================
USE `ds_order_0`;

CREATE TABLE `t_order_template` (
  `id` bigint NOT NULL COMMENT '订单ID(Snowflake)',
  `order_no` varchar(64) NOT NULL COMMENT '订单流水号',
  `user_id` bigint NOT NULL COMMENT '下单用户ID(Sharding Key)',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单实付总额',
  `status` tinyint(1) NOT NULL DEFAULT 0,
  `pay_time` datetime NULL DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '订单主表模板';

CREATE TABLE `t_order_item_template` (
  `id` bigint NOT NULL COMMENT '明细ID(Snowflake)',
  `order_no` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL COMMENT '下单用户ID(Sharding Key - Binding Table 共定位)',
  `visitor_id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `seat_id` bigint NULL DEFAULT NULL,
  `check_code` varchar(128) NOT NULL,
  `is_checked` tinyint(1) NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_check_code`(`check_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '订单明细模板';

-- 广播副本
CREATE TABLE `t_event` SELECT * FROM `ds_user_0`.`t_event` WHERE 1=0;
CREATE TABLE `t_event_config` SELECT * FROM `ds_user_0`.`t_event_config` WHERE 1=0;
CREATE TABLE `ticket_skus` (
  `id` bigint NOT NULL COMMENT '票档主键ID(Snowflake)',
  `event_id` bigint NOT NULL,
  `title` varchar(64) NOT NULL,
  `original_price` decimal(10, 2) NOT NULL,
  `selling_price` decimal(10, 2) NOT NULL,
  `total_stock` int NOT NULL,
  `remaining_stock` int NOT NULL,
  `limit_num` int NULL DEFAULT 6,
  `version` int NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_event_id`(`event_id` ASC)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '演出票档广播副本';

-- 克隆 ds_order_0 的 16 张物理表
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

-- 订单库 1
USE `ds_order_1`;
CREATE TABLE `t_order_template` SELECT * FROM `ds_order_0`.`t_order_0` WHERE 1=0;
CREATE TABLE `t_order_item_template` SELECT * FROM `ds_order_0`.`t_order_item_0` WHERE 1=0;
CREATE TABLE `t_event` SELECT * FROM `ds_user_0`.`t_event` WHERE 1=0;
CREATE TABLE `t_event_config` SELECT * FROM `ds_user_0`.`t_event_config` WHERE 1=0;
CREATE TABLE `ticket_skus` SELECT * FROM `ds_order_0`.`ticket_skus` WHERE 1=0;

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


-- =========================================================================
-- 5. 构建 ds_seat_0 和 ds_seat_1 分库与物理表
-- =========================================================================
USE `ds_seat_0`;

CREATE TABLE `t_seat_template` (
  `id` bigint NOT NULL COMMENT '座位ID(Snowflake)',
  `event_id` bigint NOT NULL COMMENT '关联演出ID(Sharding Key)',
  `sku_id` bigint NOT NULL COMMENT '关联票档ID',
  `section` varchar(32) NOT NULL COMMENT '区域',
  `row_num` int NOT NULL COMMENT '排号',
  `col_num` int NOT NULL COMMENT '列号',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '0:可选 1:锁定中 2:已售',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_sku_section`(`event_id` ASC, `sku_id` ASC, `section` ASC) USING BTREE,
  INDEX `idx_row_col`(`row_num` ASC, `col_num` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COMMENT = '演出物理座位模板';

-- 广播副本
CREATE TABLE `t_event` SELECT * FROM `ds_user_0`.`t_event` WHERE 1=0;
CREATE TABLE `ticket_skus` SELECT * FROM `ds_order_0`.`ticket_skus` WHERE 1=0;

-- 克隆 ds_seat_0 的 8 张物理表
CREATE TABLE `t_seat_0` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_1` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_2` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_3` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_4` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_5` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_6` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_7` SELECT * FROM `t_seat_template` WHERE 1=0;

DROP TABLE IF EXISTS `t_seat_template`;

-- 座位库 1
USE `ds_seat_1`;
CREATE TABLE `t_seat_template` SELECT * FROM `ds_seat_0`.`t_seat_0` WHERE 1=0;
CREATE TABLE `t_event` SELECT * FROM `ds_user_0`.`t_event` WHERE 1=0;
CREATE TABLE `ticket_skus` SELECT * FROM `ds_order_0`.`ticket_skus` WHERE 1=0;

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
