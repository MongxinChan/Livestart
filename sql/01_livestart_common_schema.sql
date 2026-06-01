/*
 =========================================================================
 🎫 LiveStart 统一主库 DDL 脚本 (01_livestart_common_schema.sql)
 =========================================================================
 
 【职责定位】
 存储系统全部非分片（单库单表）数据、元配置数据、静态字典以及结算汇总表。
 在微服务架构中，此库对应的表通常配置为 "广播表"（Broadcast Table）或 "单库默认表"，
 保证所有微服务及分片库可以随时与这些元数据表进行跨库 Join 关联查询。
 
 【实体主键策略】
 除系统配置、音乐风格等天然低频自增场景外，核心业务实体主键全部采用分布式全局唯一 ID（雪花算法 Snowflake），
 确保在分布式微服务架构及后续分库分表迁移中的数据唯一性与扩展安全。
*/

CREATE DATABASE IF NOT EXISTS `live_start` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `live_start`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_venue
-- 场馆信息表
-- ----------------------------
DROP TABLE IF EXISTS `t_venue`;
CREATE TABLE `t_venue` (
  `id` bigint NOT NULL COMMENT '场馆分布式唯一主键ID',
  `name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '场馆名称',
  `city` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '城市',
  `address` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '详细地址',
  `capacity` int DEFAULT NULL COMMENT '场馆总容纳人数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场馆信息表';

-- ----------------------------
-- Table structure for t_performer
-- 艺人/乐队信息表
-- ----------------------------
DROP TABLE IF EXISTS `t_performer`;
CREATE TABLE `t_performer` (
  `id` bigint NOT NULL COMMENT '艺人分布式唯一主键ID',
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '艺人/乐队名称',
  `style_id` bigint DEFAULT NULL COMMENT '关联风格ID (t_style.id)',
  `avatar` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '艺人头像/Logo URL',
  `bio` text COLLATE utf8mb4_unicode_ci COMMENT '艺人/乐队详细介绍',
  `status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '状态 1:正常 0:停演',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_style_id` (`style_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人/乐队信息表';

-- ----------------------------
-- Table structure for t_style
-- 音乐风格定义表
-- ----------------------------
DROP TABLE IF EXISTS `t_style`;
CREATE TABLE `t_style` (
  `id` bigint NOT NULL COMMENT '风格分布式唯一主键ID',
  `name` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '风格名称 (如: 摇滚, 民谣, 流行, 电子)',
  `code` varchar(32) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '风格代码 (用于后端逻辑标识，如 ROCK, FOLK)',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '风格描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='音乐风格定义表';

-- ----------------------------
-- Table structure for t_performer_style_relation
-- 艺人与风格关联多对多中间表
-- ----------------------------
DROP TABLE IF EXISTS `t_performer_style_relation`;
CREATE TABLE `t_performer_style_relation` (
  `performer_id` bigint NOT NULL COMMENT '艺人ID',
  `style_id` bigint NOT NULL COMMENT '风格ID',
  PRIMARY KEY (`performer_id`,`style_id`),
  KEY `idx_style_performer` (`style_id`,`performer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人风格关联多对多中间表';

-- ----------------------------
-- Table structure for t_event
-- 演出基础信息表
-- ----------------------------
DROP TABLE IF EXISTS `t_event`;
CREATE TABLE `t_event` (
  `id` bigint NOT NULL COMMENT '演出分布式唯一主键ID',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '演出标题',
  `event_type` tinyint(1) NOT NULL COMMENT '演出类型 0:Livehouse(站票) 1:演唱会(选座/ABCD区)',
  `venue_id` bigint NOT NULL COMMENT '关联场馆ID',
  `start_time` datetime NOT NULL COMMENT '演出开始时间',
  `poster_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '海报图片地址',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态 0:下架 1:预售 2:在售 3:售罄',
  PRIMARY KEY (`id`),
  KEY `idx_venue_id` (`venue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出基础信息表';

-- ----------------------------
-- Table structure for t_event_config
-- 演出业务配置及退票策略表
-- ----------------------------
DROP TABLE IF EXISTS `t_event_config`;
CREATE TABLE `t_event_config` (
  `event_id` bigint NOT NULL COMMENT '对应演出ID',
  `selection_mode` tinyint(1) NOT NULL DEFAULT '0' COMMENT '选座模式 0:系统自动配座(高并发抢票) 1:手动选座(剧场/音乐剧)',
  `is_verify_required` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否强制实名制入场 0:否 1:是',
  `max_tickets_per_user` int DEFAULT '6' COMMENT '单人账户最大购票上限',
  `refund_policy_type` tinyint(1) NOT NULL DEFAULT '0' COMMENT '退票政策 0:不可退 1:全额退 2:阶梯退票',
  `tier1_free_refund_hours` int DEFAULT NULL COMMENT '全额退票截止时间(开演前X小时)',
  `tier2_partial_refund_hours` int DEFAULT NULL COMMENT '部分退票截止时间(开演前Y小时)',
  `tier2_refund_fee_rate` decimal(5,2) DEFAULT '0.00' COMMENT '部分退票手续费比例 (0.20 代表扣除20%手续费)',
  `is_transferable` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否允许转赠门票 0:否 1:是',
  `is_waiting_allowed` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否开启候补购票功能',
  PRIMARY KEY (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出业务配置及退票策略表';

-- ----------------------------
-- Table structure for ticket_skus
-- 演出票档库存表
-- ----------------------------
DROP TABLE IF EXISTS `ticket_skus`;
CREATE TABLE `ticket_skus` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '票档主键ID(分布式雪花/自增安全)',
  `event_id` bigint NOT NULL COMMENT '关联演出ID',
  `title` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '票种名称: 如 680元档/VIP区/早鸟',
  `original_price` decimal(10,2) NOT NULL COMMENT '原价',
  `selling_price` decimal(10,2) NOT NULL COMMENT '售价',
  `total_stock` int NOT NULL COMMENT '总库存',
  `remaining_stock` int NOT NULL COMMENT '当前剩余物理库存',
  `limit_num` int DEFAULT '6' COMMENT '单人单次限购数量',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号(防超卖高并发校验)',
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出票档库存表';

-- ----------------------------
-- Table structure for t_comment
-- 评论回复表
-- ----------------------------
DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment` (
  `id` bigint NOT NULL COMMENT '评论唯一ID',
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` text COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `status` tinyint DEFAULT '0' COMMENT '审核状态 0:待审 1:通过 2:屏蔽',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_event_id` (`event_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论回复表';

-- ----------------------------
-- Table structure for t_refund_policy
-- 阶梯退票费率配置表
-- ----------------------------
DROP TABLE IF EXISTS `t_refund_policy`;
CREATE TABLE `t_refund_policy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '策略ID',
  `event_id` bigint NOT NULL COMMENT '关联演出ID',
  `is_allow_refund` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否允许退票 0:全场不可退 1:允许退票',
  `tier1_deadline_hours` int DEFAULT '48' COMMENT '全额退截止时间(距离开演前X小时)',
  `tier2_deadline_hours` int DEFAULT '24' COMMENT '部分退截止时间(距离开演前Y小时)',
  `tier2_refund_fee_rate` decimal(5,2) DEFAULT '0.20' COMMENT '部分退手续费比例 (0.20 代表扣除实付金额的20%)',
  `policy_desc` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '退票政策文案描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='阶梯退票费率配置表';

-- ----------------------------
-- Table structure for t_settlement
-- 主办方演出票房结算单
-- ----------------------------
DROP TABLE IF EXISTS `t_settlement`;
CREATE TABLE `t_settlement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `event_title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '演出名称',
  `total_tickets` int NOT NULL DEFAULT '0' COMMENT '总出票数',
  `total_sales_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总销售额',
  `commission_rate` decimal(5,4) NOT NULL DEFAULT '0.0500' COMMENT '平台扣点比例 (默认 5%)',
  `commission_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '扣除佣金金额',
  `settlement_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '商家应结金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '结算状态 0:未结算 1:已结算 2:结算异常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主办方演出票房结算单';

-- ----------------------------
-- Table structure for t_operation_log
-- 系统操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作用户名',
  `operation` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作名称',
  `method` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `params` text COLLATE utf8mb4_unicode_ci COMMENT '请求参数',
  `execution_time` bigint DEFAULT NULL COMMENT '执行时长(毫秒)',
  `ip` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人IP',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ----------------------------
-- Table structure for t_user_phone_mapping
-- 用户手机号与全局ID映射路由表
-- ----------------------------
DROP TABLE IF EXISTS `t_user_phone_mapping`;
CREATE TABLE `t_user_phone_mapping` (
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '手机号(唯一索引 & 登录凭证)',
  `user_id` bigint NOT NULL COMMENT '关联分布式用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`phone`),
  UNIQUE KEY `idx_unique_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户手机号与全局ID映射路由表';

SET FOREIGN_KEY_CHECKS = 1;
