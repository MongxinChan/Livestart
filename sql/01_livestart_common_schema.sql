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

DROP DATABASE IF EXISTS `live_start`;
CREATE DATABASE `live_start` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
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
  `owner_user_id` bigint DEFAULT NULL COMMENT '归属场地管理员用户ID（关联 t_user.id, user_type=3），NULL 表示无归属（公共场馆/超管管理）',
  PRIMARY KEY (`id`),
  KEY `idx_owner_user_id` (`owner_user_id`)
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
-- Table structure for t_event_style_relation
-- 演出与风格关联多对多中间表
-- ----------------------------
DROP TABLE IF EXISTS `t_event_style_relation`;
CREATE TABLE `t_event_style_relation` (
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `style_id` bigint NOT NULL COMMENT '风格ID',
  PRIMARY KEY (`event_id`,`style_id`),
  KEY `idx_style_event` (`style_id`,`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出风格关联多对多中间表';


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
-- Table structure for t_ticket_sku
-- 演出票档库存表
-- ----------------------------
DROP TABLE IF EXISTS `t_ticket_sku`;
CREATE TABLE `t_ticket_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '票档主键ID(分布式雪花/自增安全)',
  `event_id` bigint NOT NULL COMMENT '关联演出ID',
  `title` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '票种名称: 如 680元档/VIP区/早鸟',
  `original_price` decimal(10,2) NOT NULL COMMENT '原价',
  `selling_price` decimal(10,2) NOT NULL COMMENT '售价',
  `total_stock` int NOT NULL COMMENT '总库存',
  `stage1_stock` int DEFAULT NULL COMMENT '一开释放库存',
  `stage2_stock` int DEFAULT NULL COMMENT '二开释放库存',
  `stage2_released` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'stage2 stock released flag',
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

-- ----------------------------
-- Records of t_style
-- 初始化风格数据
-- ----------------------------
INSERT INTO `t_style` (`id`, `name`, `code`, `description`) VALUES
(1, '流行', 'POP', '当代流行音乐，旋律优美、节奏明快'),
(2, '华语流行', 'CPOP', '华语地区主流流行音乐'),
(3, '欧美流行', 'WESTERN_POP', '欧美地区主流流行音乐'),
(4, '日韩流行', 'JPKPOP', '日本与韩国流行音乐（J-POP / K-POP）'),
(5, '摇滚', 'ROCK', '以吉他、贝斯、鼓为核心的摇滚乐'),
(6, '独立摇滚', 'INDIE_ROCK', '独立厂牌发行的另类摇滚风格'),
(7, '朋克', 'PUNK', '快速节奏、简洁编曲的朋克摇滚'),
(8, '金属', 'METAL', '重型吉他 riff 与高强度打击乐'),
(9, '嘻哈', 'HIPHOP', '说唱与节拍驱动的嘻哈音乐'),
(10, '说唱', 'RAP', '以韵律念白为主的说唱表演'),
(11, '电子', 'ELECTRONIC', '电子合成器与数字制作的音乐风格'),
(12, 'EDM', 'EDM', '电子舞曲，适合大型音乐节'),
(13, 'Techno', 'TECHNO', '重复性节拍驱动的电子音乐'),
(14, 'House', 'HOUSE', '四拍底鼓驱动的电子舞曲'),
(15, 'R&B', 'RNB', '节奏布鲁斯，融合灵魂与流行元素'),
(16, '灵魂乐', 'SOUL', '源自福音音乐的深情演唱风格'),
(17, '民谣', 'FOLK', '原声吉他与叙事性歌词的民谣音乐'),
(18, '乡村', 'COUNTRY', '美式乡村音乐风格'),
(19, '爵士', 'JAZZ', '即兴演奏与复杂和声的爵士乐'),
(20, '布鲁斯', 'BLUES', '源自非裔美国人音乐传统的蓝调'),
(21, '古典', 'CLASSICAL', '西方古典音乐，含交响乐与室内乐'),
(22, '民族', 'ETHNIC', '融合各民族传统元素的音乐风格'),
(23, '国风', 'CHINESE_STYLE', '融合中国传统乐器与现代编曲'),
(24, '戏曲', 'OPERA', '中国传统戏曲艺术表演'),
(25, '雷鬼', 'REGGAE', '牙买加风格的节奏音乐'),
(26, '拉丁', 'LATIN', '拉丁美洲风格 of 音乐与舞蹈'),
(27, 'Bossa Nova', 'BOSSA_NOVA', '巴西风格的柔和爵士融合'),
(28, '脱口秀', 'TALK_SHOW', '单人或多人喜剧脱口秀表演'),
(29, '相声', 'CROSSTALK', '中国传统语言类曲艺表演'),
(30, '音乐剧', 'MUSICAL', '融合歌唱、对白与舞蹈的舞台剧'),
(31, '话剧', 'DRAMA', '以对话和表演为主的戏剧'),
(32, '舞蹈', 'DANCE', '各类舞蹈表演（现代舞、芭蕾等）'),
(33, '魔术', 'MAGIC', '魔术与幻术表演'),
(34, '综合演出', 'VARIETY', '多种表演形式混合的综艺类演出');

SET FOREIGN_KEY_CHECKS = 1;
