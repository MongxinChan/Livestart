/*
 * LiveStart 全新环境一体化初始化脚本（唯一初始化入口）。
 *
 * 适用：全新本地/演示环境。脚本会重建 live_start、分片库、xxl_job 和默认单库支付库，禁止对已有业务库执行。
 * 默认支付模式：单库 live_start_pay。需要支付分片和演示数据时，再执行 02_livestart_optional_extras.sql。
 * 已有数据库升级请使用 01_livestart_existing_db_upgrade.sql，不要执行本文件；测试数据包含在 02_livestart_optional_extras.sql。
 */

-- ============================================================================
-- BEGIN 01_livestart_common_schema.sql
-- ============================================================================
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
  UNIQUE KEY `idx_owner_user_id` (`owner_user_id`)
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
  `source_event_id` bigint DEFAULT NULL COMMENT '分销演出对应的商户原始演出ID',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '演出标题',
  `event_type` tinyint(1) NOT NULL COMMENT '演出类型 0:Livehouse(站票) 1:演唱会(选座/ABCD区)',
  `venue_id` bigint NOT NULL COMMENT '关联场馆ID',
  `start_time` datetime NOT NULL COMMENT '演出开始时间',
  `poster_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '海报图片地址',
  `status` tinyint(1) DEFAULT '1' COMMENT '状态 0:下架 1:预售 2:在售 3:售罄',
  PRIMARY KEY (`id`),
  KEY `idx_venue_id` (`venue_id`),
  UNIQUE KEY `uq_source_event_id` (`source_event_id`)
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
  `error_message` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结算异常信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_event` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主办方演出票房结算单';

DROP TABLE IF EXISTS `t_settlement_notification_read`;
CREATE TABLE `t_settlement_notification_read` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '后台用户ID',
  `notification_key` varchar(120) NOT NULL COMMENT '通知唯一键',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_notification` (`user_id`, `notification_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算通知已读记录';

-- ----------------------------
-- Table structure for t_operation_log
-- 系统操作日志表
-- ----------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日志租户',
  `operator_name` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人名称',
  `operation_log` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作日志描述',
  `original_data` text COLLATE utf8mb4_unicode_ci COMMENT '原始数据(JSON)',
  `type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型',
  `sub_type` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作子类型',
  `biz_no` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '业务编号',
  `modified_data` text COLLATE utf8mb4_unicode_ci COMMENT '修改后数据(JSON)',
  `operator_id` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作人ID',
  `fail` tinyint NOT NULL DEFAULT '0' COMMENT '是否失败日志',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_biz_no` (`type`,`biz_no`),
  KEY `idx_create_time` (`create_time`)
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

-- END 01_livestart_common_schema.sql

-- ============================================================================
-- BEGIN 02_livestart_sharded_schema.sql
-- ============================================================================
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
 - 用户、订单库中冗余了常用的公共表副本（如 t_event, t_ticket_sku）。
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
  `source_event_id` bigint DEFAULT NULL,
  `title` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `event_type` tinyint(1) NOT NULL,
  `venue_id` bigint NOT NULL,
  `start_time` datetime NOT NULL,
  `poster_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `status` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `idx_source_event_id` (`source_event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='演出表广播副本';

-- 物理克隆分片表 0..7
CREATE TABLE `t_user_0` LIKE `t_user_template`;
CREATE TABLE `t_user_1` LIKE `t_user_template`;
CREATE TABLE `t_user_2` LIKE `t_user_template`;
CREATE TABLE `t_user_3` LIKE `t_user_template`;
CREATE TABLE `t_user_4` LIKE `t_user_template`;
CREATE TABLE `t_user_5` LIKE `t_user_template`;
CREATE TABLE `t_user_6` LIKE `t_user_template`;
CREATE TABLE `t_user_7` LIKE `t_user_template`;

CREATE TABLE `t_user_profile_0` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_1` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_2` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_3` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_4` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_5` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_6` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_7` LIKE `t_user_profile_template`;

CREATE TABLE `t_user_visitor_0` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_1` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_2` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_3` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_4` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_5` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_6` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_7` LIKE `t_user_visitor_template`;

DROP TABLE IF EXISTS `t_user_template`;
DROP TABLE IF EXISTS `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_visitor_template`;


-- 用户分片库 1
USE `ds_user_1`;

CREATE TABLE `t_user_template` LIKE `ds_user_0`.`t_user_0`;
CREATE TABLE `t_user_profile_template` LIKE `ds_user_0`.`t_user_profile_0`;
CREATE TABLE `t_user_visitor_template` LIKE `ds_user_0`.`t_user_visitor_0`;
CREATE TABLE `t_event` LIKE `ds_user_0`.`t_event`;

CREATE TABLE `t_user_0` LIKE `t_user_template`;
CREATE TABLE `t_user_1` LIKE `t_user_template`;
CREATE TABLE `t_user_2` LIKE `t_user_template`;
CREATE TABLE `t_user_3` LIKE `t_user_template`;
CREATE TABLE `t_user_4` LIKE `t_user_template`;
CREATE TABLE `t_user_5` LIKE `t_user_template`;
CREATE TABLE `t_user_6` LIKE `t_user_template`;
CREATE TABLE `t_user_7` LIKE `t_user_template`;

CREATE TABLE `t_user_profile_0` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_1` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_2` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_3` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_4` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_5` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_6` LIKE `t_user_profile_template`;
CREATE TABLE `t_user_profile_7` LIKE `t_user_profile_template`;

CREATE TABLE `t_user_visitor_0` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_1` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_2` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_3` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_4` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_5` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_6` LIKE `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_7` LIKE `t_user_visitor_template`;

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
  `status` tinyint(1) NOT NULL DEFAULT '0' COMMENT '状态 0:待支付 1:已支付 2:已取消 3:已退票',
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
  `checked_at` datetime DEFAULT NULL COMMENT '成功核销时间',
  `checked_by` bigint DEFAULT NULL COMMENT '核销操作人用户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_unique_check_code` (`check_code`),
  KEY `idx_checked_at` (`checked_at`),
  KEY `idx_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细模板';

-- 广播表副本，极速联查票价
CREATE TABLE `t_event` LIKE `ds_user_0`.`t_event`;

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
CREATE TABLE `t_order_0` LIKE `t_order_template`;
CREATE TABLE `t_order_1` LIKE `t_order_template`;
CREATE TABLE `t_order_2` LIKE `t_order_template`;
CREATE TABLE `t_order_3` LIKE `t_order_template`;
CREATE TABLE `t_order_4` LIKE `t_order_template`;
CREATE TABLE `t_order_5` LIKE `t_order_template`;
CREATE TABLE `t_order_6` LIKE `t_order_template`;
CREATE TABLE `t_order_7` LIKE `t_order_template`;
CREATE TABLE `t_order_8` LIKE `t_order_template`;
CREATE TABLE `t_order_9` LIKE `t_order_template`;
CREATE TABLE `t_order_10` LIKE `t_order_template`;
CREATE TABLE `t_order_11` LIKE `t_order_template`;
CREATE TABLE `t_order_12` LIKE `t_order_template`;
CREATE TABLE `t_order_13` LIKE `t_order_template`;
CREATE TABLE `t_order_14` LIKE `t_order_template`;
CREATE TABLE `t_order_15` LIKE `t_order_template`;

-- 物理克隆分片明细表 t_order_item_0..15
CREATE TABLE `t_order_item_0` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_1` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_2` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_3` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_4` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_5` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_6` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_7` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_8` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_9` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_10` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_11` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_12` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_13` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_14` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_15` LIKE `t_order_item_template`;

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
CREATE TABLE `t_user_ticket_0` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_1` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_2` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_3` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_4` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_5` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_6` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_7` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_8` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_9` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_10` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_11` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_12` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_13` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_14` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_15` LIKE `t_user_ticket_template`;

DROP TABLE IF EXISTS `t_user_ticket_template`;


-- 订单分片库 1
USE `ds_order_1`;

CREATE TABLE `t_order_template` LIKE `ds_order_0`.`t_order_0`;
CREATE TABLE `t_order_item_template` LIKE `ds_order_0`.`t_order_item_0`;
CREATE TABLE `t_event` LIKE `ds_order_0`.`t_event`;
CREATE TABLE `t_ticket_sku` LIKE `ds_order_0`.`t_ticket_sku`;

CREATE TABLE `t_order_0` LIKE `t_order_template`;
CREATE TABLE `t_order_1` LIKE `t_order_template`;
CREATE TABLE `t_order_2` LIKE `t_order_template`;
CREATE TABLE `t_order_3` LIKE `t_order_template`;
CREATE TABLE `t_order_4` LIKE `t_order_template`;
CREATE TABLE `t_order_5` LIKE `t_order_template`;
CREATE TABLE `t_order_6` LIKE `t_order_template`;
CREATE TABLE `t_order_7` LIKE `t_order_template`;
CREATE TABLE `t_order_8` LIKE `t_order_template`;
CREATE TABLE `t_order_9` LIKE `t_order_template`;
CREATE TABLE `t_order_10` LIKE `t_order_template`;
CREATE TABLE `t_order_11` LIKE `t_order_template`;
CREATE TABLE `t_order_12` LIKE `t_order_template`;
CREATE TABLE `t_order_13` LIKE `t_order_template`;
CREATE TABLE `t_order_14` LIKE `t_order_template`;
CREATE TABLE `t_order_15` LIKE `t_order_template`;

CREATE TABLE `t_order_item_0` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_1` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_2` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_3` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_4` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_5` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_6` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_7` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_8` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_9` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_10` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_11` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_12` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_13` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_14` LIKE `t_order_item_template`;
CREATE TABLE `t_order_item_15` LIKE `t_order_item_template`;

DROP TABLE IF EXISTS `t_order_template`;
DROP TABLE IF EXISTS `t_order_item_template`;

-- 用户票务分片表（从 ds_order_0 克隆结构）
CREATE TABLE `t_user_ticket_template` LIKE `ds_order_0`.`t_user_ticket_0`;

CREATE TABLE `t_user_ticket_0` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_1` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_2` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_3` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_4` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_5` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_6` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_7` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_8` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_9` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_10` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_11` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_12` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_13` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_14` LIKE `t_user_ticket_template`;
CREATE TABLE `t_user_ticket_15` LIKE `t_user_ticket_template`;

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

-- 物理克隆分片座位表 t_seat_0..7
CREATE TABLE `t_seat_0` LIKE `t_seat_template`;
CREATE TABLE `t_seat_1` LIKE `t_seat_template`;
CREATE TABLE `t_seat_2` LIKE `t_seat_template`;
CREATE TABLE `t_seat_3` LIKE `t_seat_template`;
CREATE TABLE `t_seat_4` LIKE `t_seat_template`;
CREATE TABLE `t_seat_5` LIKE `t_seat_template`;
CREATE TABLE `t_seat_6` LIKE `t_seat_template`;
CREATE TABLE `t_seat_7` LIKE `t_seat_template`;

DROP TABLE IF EXISTS `t_seat_template`;


-- 座位分片库 1
USE `ds_seat_1`;

CREATE TABLE `t_seat_template` LIKE `ds_seat_0`.`t_seat_0`;

CREATE TABLE `t_seat_0` LIKE `t_seat_template`;
CREATE TABLE `t_seat_1` LIKE `t_seat_template`;
CREATE TABLE `t_seat_2` LIKE `t_seat_template`;
CREATE TABLE `t_seat_3` LIKE `t_seat_template`;
CREATE TABLE `t_seat_4` LIKE `t_seat_template`;
CREATE TABLE `t_seat_5` LIKE `t_seat_template`;
CREATE TABLE `t_seat_6` LIKE `t_seat_template`;
CREATE TABLE `t_seat_7` LIKE `t_seat_template`;

DROP TABLE IF EXISTS `t_seat_template`;

SET FOREIGN_KEY_CHECKS = 1;

-- END 02_livestart_sharded_schema.sql

-- ============================================================================
-- BEGIN 03_tables_xxl_job.sql
-- ============================================================================
/*
 =========================================================================
 🎫 LiveStart XXL-JOB 任务调度服务建表脚本 (03_tables_xxl_job.sql)
 =========================================================================

 【职责定位】
 XXL-JOB 分布式任务调度服务（中心端 xxl-job-admin）所需的 8 张基础核心元数据表。
 用于管理演唱会售票平台的票务库存准点预热、超时订单核对等各种后台定时任务。
*/

DROP DATABASE IF EXISTS `xxl_job`;
CREATE DATABASE `xxl_job` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `xxl_job`;

SET NAMES utf8mb4;

-- ----------------------------
-- Table structure for xxl_job_group
-- 执行器组表
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_group`;
CREATE TABLE `xxl_job_group` (
  `id` int NOT NULL AUTO_INCREMENT,
  `app_name` varchar(64) NOT NULL COMMENT '执行器AppName',
  `title` varchar(64) NOT NULL COMMENT '执行器名称',
  `address_type` tinyint NOT NULL DEFAULT '0' COMMENT '执行器地址类型：0=自动注册、1=手动录入',
  `address_list` text COMMENT '执行器地址列表，多地址逗号分隔',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行器组表';

-- ----------------------------
-- Table structure for xxl_job_registry
-- 执行器注册表
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_registry`;
CREATE TABLE `xxl_job_registry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `registry_group` varchar(50) NOT NULL,
  `registry_key` varchar(255) NOT NULL,
  `registry_value` varchar(255) NOT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_g_k_v` (`registry_group`,`registry_key`,`registry_value`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行器注册表';

-- ----------------------------
-- Table structure for xxl_job_info
-- 调度任务信息表
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_info`;
CREATE TABLE `xxl_job_info` (
  `id` int NOT NULL AUTO_INCREMENT,
  `job_group` int NOT NULL COMMENT '执行器主键ID',
  `job_desc` varchar(255) NOT NULL,
  `add_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `author` varchar(64) DEFAULT NULL COMMENT '作者',
  `alarm_email` varchar(255) DEFAULT NULL COMMENT '报警邮件',
  `schedule_type` varchar(50) NOT NULL DEFAULT 'NONE' COMMENT '调度类型',
  `schedule_conf` varchar(128) DEFAULT NULL COMMENT '调度配置，值含义取决于调度类型',
  `misfire_strategy` varchar(50) NOT NULL DEFAULT 'DO_NOTHING' COMMENT '调度过期策略',
  `executor_route_strategy` varchar(50) DEFAULT NULL COMMENT '执行器路由策略',
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '任务handler',
  `executor_param` text COMMENT '任务参数',
  `executor_block_strategy` varchar(50) DEFAULT NULL COMMENT '阻塞处理策略',
  `executor_timeout` int NOT NULL DEFAULT '0' COMMENT '任务执行超时时间，单位秒',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `glue_type` varchar(50) NOT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) DEFAULT NULL COMMENT 'GLUE备注',
  `glue_updatetime` datetime DEFAULT NULL COMMENT 'GLUE更新时间',
  `child_jobid` varchar(255) DEFAULT NULL COMMENT '子任务ID，多个逗号分隔',
  `trigger_status` tinyint NOT NULL DEFAULT '0' COMMENT '调度状态：0-停止，1-运行',
  `trigger_last_time` bigint NOT NULL DEFAULT '0' COMMENT '上次调度时间',
  `trigger_next_time` bigint NOT NULL DEFAULT '0' COMMENT '下次调度时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度任务信息表';

-- ----------------------------
-- Table structure for xxl_job_logglue
-- 任务GLUE日志表
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_logglue`;
CREATE TABLE `xxl_job_logglue` (
  `id` int NOT NULL AUTO_INCREMENT,
  `job_id` int NOT NULL COMMENT '任务，主键ID',
  `glue_type` varchar(50) DEFAULT NULL COMMENT 'GLUE类型',
  `glue_source` mediumtext COMMENT 'GLUE源代码',
  `glue_remark` varchar(128) NOT NULL COMMENT 'GLUE备注',
  `add_time` datetime DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务GLUE日志表';

-- ----------------------------
-- Table structure for xxl_job_log
-- 调度任务日志表
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_log`;
CREATE TABLE `xxl_job_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `job_group` int NOT NULL COMMENT '执行器主键ID',
  `job_id` int NOT NULL COMMENT '任务，主键ID',
  `executor_address` varchar(255) DEFAULT NULL COMMENT '执行器地址，本次执行的地址',
  `executor_handler` varchar(255) DEFAULT NULL COMMENT '任务handler',
  `executor_param` text COMMENT '任务参数',
  `executor_sharding_param` varchar(20) DEFAULT NULL COMMENT '任务分片参数，格式如 1/2',
  `executor_fail_retry_count` int NOT NULL DEFAULT '0' COMMENT '失败重试次数',
  `trigger_time` datetime DEFAULT NULL COMMENT '调度-时间',
  `trigger_code` int NOT NULL COMMENT '调度-结果',
  `trigger_msg` text COMMENT '调度-日志',
  `handle_time` datetime DEFAULT NULL COMMENT '执行-时间',
  `handle_code` int NOT NULL COMMENT '执行-状态',
  `handle_msg` text COMMENT '执行-日志',
  `alarm_status` tinyint NOT NULL DEFAULT '0' COMMENT '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败',
  PRIMARY KEY (`id`),
  KEY `I_trigger_time` (`trigger_time`),
  KEY `I_handle_code` (`handle_code`),
  KEY `I_jobgroup` (`job_group`),
  KEY `I_jobid` (`job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度任务日志表';

-- ----------------------------
-- Table structure for xxl_job_log_report
-- 调度任务日志报表
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_log_report`;
CREATE TABLE `xxl_job_log_report` (
  `id` int NOT NULL AUTO_INCREMENT,
  `trigger_day` datetime DEFAULT NULL COMMENT '调度-时间',
  `running_count` int NOT NULL DEFAULT '0' COMMENT '运行中-日志数量',
  `suc_count` int NOT NULL DEFAULT '0' COMMENT '执行成功-日志数量',
  `fail_count` int NOT NULL DEFAULT '0' COMMENT '执行失败-日志数量',
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_trigger_day` (`trigger_day`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度任务日志报表';

-- ----------------------------
-- Table structure for xxl_job_lock
-- 任务调度锁表
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_lock`;
CREATE TABLE `xxl_job_lock` (
  `lock_name` varchar(50) NOT NULL COMMENT '锁名称',
  PRIMARY KEY (`lock_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务调度锁表';

-- ----------------------------
-- Table structure for xxl_job_user
-- 任务调度系统用户表
-- ----------------------------
DROP TABLE IF EXISTS `xxl_job_user`;
CREATE TABLE `xxl_job_user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL COMMENT '账号',
  `password` varchar(100) NOT NULL COMMENT '密码加密信息',
  `token` varchar(100) DEFAULT NULL COMMENT '登录token',
  `role` tinyint NOT NULL COMMENT '角色：0-普通用户、1-管理员',
  `permission` varchar(255) DEFAULT NULL COMMENT '权限：执行器ID列表，多个逗号分割',
  PRIMARY KEY (`id`),
  UNIQUE KEY `i_username` (`username`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务调度系统用户表';

-- ----------------------------
-- 初始化默认数据
-- ----------------------------
INSERT INTO `xxl_job_group`(`id`, `app_name`, `title`, `address_type`, `address_list`, `update_time`)
    VALUES (1, 'livestart-distribution-executor', 'Livestart Distribution Executor', 0, NULL, now());

INSERT INTO `xxl_job_info`(`id`, `job_group`, `job_desc`, `add_time`, `update_time`, `author`, `alarm_email`,
                           `schedule_type`, `schedule_conf`, `misfire_strategy`, `executor_route_strategy`,
                           `executor_handler`, `executor_param`, `executor_block_strategy`, `executor_timeout`,
                           `executor_fail_retry_count`, `glue_type`, `glue_source`, `glue_remark`, `glue_updatetime`,
                           `child_jobid`)
VALUES (1, 1, '示例任务01', now(), now(), 'XXL', '', 'CRON', '0 0 0 * * ? *',
        'DO_NOTHING', 'FIRST', 'demoJobHandler', '', 'SERIAL_EXECUTION', 0, 0, 'BEAN', '', 'GLUE代码初始化',
        now(), '');

INSERT INTO `xxl_job_user`(`id`, `username`, `password`, `role`, `permission`)
VALUES (1, 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL);

INSERT INTO `xxl_job_lock` (`lock_name`)
VALUES ('schedule_lock');

COMMIT;

-- END 03_tables_xxl_job.sql

-- ============================================================================
-- BEGIN 04_feature_and_job_upgrade.sql
-- ============================================================================
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

USE `live_start`;

DELIMITER //

DROP PROCEDURE IF EXISTS `sp_apply_feature_and_job_upgrade`//
CREATE PROCEDURE `sp_apply_feature_and_job_upgrade`()
BEGIN

  -- 0. Ensure venue ownership is available and remains one-to-one.
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_venue' AND COLUMN_NAME = 'owner_user_id'
  ) THEN
    ALTER TABLE `live_start`.`t_venue`
      ADD COLUMN `owner_user_id` bigint DEFAULT NULL COMMENT '归属场地管理员用户ID' AFTER `capacity`;
  END IF;

  IF EXISTS (
    SELECT 1 FROM `live_start`.`t_venue`
    WHERE `owner_user_id` IS NOT NULL
    GROUP BY `owner_user_id`
    HAVING COUNT(*) > 1
  ) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 't_venue contains duplicate owner_user_id values; clean them before upgrading';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_venue'
      AND INDEX_NAME = 'idx_owner_user_id' AND NON_UNIQUE = 1
  ) THEN
    ALTER TABLE `live_start`.`t_venue` DROP INDEX `idx_owner_user_id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_venue'
      AND INDEX_NAME = 'idx_owner_user_id' AND NON_UNIQUE = 0
  ) THEN
    ALTER TABLE `live_start`.`t_venue`
      ADD UNIQUE KEY `idx_owner_user_id` (`owner_user_id`);
  END IF;

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
      CHANGE COLUMN `operation` `operation_log` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作日志描述';
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log'
      AND COLUMN_NAME = 'operation_log' AND CHARACTER_MAXIMUM_LENGTH < 512
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      MODIFY COLUMN `operation_log` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作日志描述';
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
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'tenant'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      ADD COLUMN `tenant` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '日志租户' AFTER `id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'sub_type'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      ADD COLUMN `sub_type` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作子类型' AFTER `type`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_operation_log' AND COLUMN_NAME = 'fail'
  ) THEN
    ALTER TABLE `live_start`.`t_operation_log`
      ADD COLUMN `fail` tinyint NOT NULL DEFAULT 0 COMMENT '是否失败日志' AFTER `operator_id`;
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
  INSERT IGNORE INTO `live_start`.`t_event_ticket_stage` (`event_id`, `ticket_stage`)
  SELECT `id`, 1
  FROM `live_start`.`t_event`
  WHERE `id` IS NOT NULL;

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

-- END 04_feature_and_job_upgrade.sql

-- ============================================================================
-- BEGIN 09_distribution_tables_upgrade.sql
-- ============================================================================
/*
  Distribution module tables missing from the original common schema.
  Run after 01_livestart_common_schema.sql and 04_feature_and_job_upgrade.sql.
  Idempotent: never drops existing tables.
*/

USE `live_start`;

-- 查询接口不承担数据迁移；在升级阶段一次性回填艺人旧版单风格字段。
INSERT IGNORE INTO `t_performer_style_relation` (`performer_id`, `style_id`)
SELECT `id`, `style_id`
FROM `t_performer`
WHERE `style_id` IS NOT NULL;

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

-- END 09_distribution_tables_upgrade.sql

-- ============================================================================
-- BEGIN 10_refund_stock_compensation.sql
-- ============================================================================
-- 退款库存回补任务表增量脚本。
-- 执行库：live_start
USE `live_start`;

CREATE TABLE IF NOT EXISTS `t_stock_restore_task` (
  `id` bigint NOT NULL,
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型：REFUND 或 TIMEOUT_CLOSE',
  `order_no` varchar(64) NOT NULL COMMENT '业务订单号',
  `user_id` bigint NOT NULL,
  `event_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `restore_count` int NOT NULL,
  `refund_confirmed` tinyint NOT NULL DEFAULT '0' COMMENT '支付服务是否已确认退款成功',
  `db_restored` tinyint NOT NULL DEFAULT '0' COMMENT '数据库库存是否已回补',
  `redis_restored` tinyint NOT NULL DEFAULT '0' COMMENT 'Redis库存是否已回补',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0:待处理 1:完成',
  `retry_count` int NOT NULL DEFAULT '0',
  `next_retry_time` datetime DEFAULT NULL,
  `last_error` varchar(512) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stock_restore_biz_order` (`biz_type`, `order_no`),
  KEY `idx_stock_restore_pending` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='退款与超时关单库存回补任务';

SET @stock_restore_refund_confirmed_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'live_start'
    AND TABLE_NAME = 't_stock_restore_task'
    AND COLUMN_NAME = 'refund_confirmed'
);
SET @stock_restore_refund_confirmed_ddl := IF(
  @stock_restore_refund_confirmed_exists = 0,
  'ALTER TABLE `t_stock_restore_task` ADD COLUMN `refund_confirmed` tinyint NOT NULL DEFAULT 0 COMMENT ''支付服务是否已确认退款成功'' AFTER `restore_count`',
  'SELECT 1'
);
PREPARE stock_restore_refund_confirmed_stmt FROM @stock_restore_refund_confirmed_ddl;
EXECUTE stock_restore_refund_confirmed_stmt;
DEALLOCATE PREPARE stock_restore_refund_confirmed_stmt;

-- END 10_refund_stock_compensation.sql

-- ============================================================================
-- BEGIN 13_distribution_event_source_link.sql
-- ============================================================================
-- Link the merchant event shown in search to its distribution sale stages.
-- Run after 04_feature_and_job_upgrade.sql on existing databases.
USE `live_start`;

DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_distribution_event_source_link$$
CREATE PROCEDURE upgrade_distribution_event_source_link()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'source_event_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `source_event_id` bigint NULL COMMENT 'Merchant event ID for distribution copy' AFTER `id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND INDEX_NAME IN ('idx_source_event_id', 'uq_source_event_id')
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD INDEX `idx_source_event_id` (`source_event_id`);
  END IF;

  -- Only backfill a published copy when exactly one merchant event matches.
  DROP TEMPORARY TABLE IF EXISTS distribution_event_source_matches;
  CREATE TEMPORARY TABLE distribution_event_source_matches AS
    SELECT d.id AS distribution_id, MIN(m.id) AS merchant_id
    FROM `live_start`.`t_event` AS d
    JOIN `live_start`.`t_event_sale_stage` AS stage ON stage.event_id = d.id
    JOIN `live_start`.`t_event` AS m
      ON m.id <> d.id AND m.title = d.title
      AND m.venue_id = d.venue_id AND m.start_time = d.start_time
    LEFT JOIN `live_start`.`t_event_sale_stage` AS merchant_stage ON merchant_stage.event_id = m.id
    WHERE d.source_event_id IS NULL AND merchant_stage.id IS NULL
    GROUP BY d.id
    HAVING COUNT(DISTINCT m.id) = 1;

  UPDATE `live_start`.`t_event` AS published
  JOIN distribution_event_source_matches AS matches ON matches.distribution_id = published.id
  SET published.source_event_id = matches.merchant_id;
  DROP TEMPORARY TABLE distribution_event_source_matches;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND INDEX_NAME = 'uq_source_event_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD UNIQUE INDEX `uq_source_event_id` (`source_event_id`);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND INDEX_NAME = 'idx_source_event_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event` DROP INDEX `idx_source_event_id`;
  END IF;
END$$
CALL upgrade_distribution_event_source_link()$$
DROP PROCEDURE upgrade_distribution_event_source_link$$
DELIMITER ;

-- END 13_distribution_event_source_link.sql

-- ============================================================================
-- BEGIN 14_verify_ticket_lookup_indexes.sql
-- ============================================================================
-- Existing order shards created with CREATE TABLE AS SELECT have no indexes.
-- Run once after 02_livestart_sharded_schema.sql; safe to rerun.
USE `live_start`;
DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_ticket_lookup_indexes$$
CREATE PROCEDURE upgrade_ticket_lookup_indexes()
BEGIN
  DECLARE database_index INT DEFAULT 0;
  DECLARE table_index INT;
  DECLARE schema_name VARCHAR(32);
  DECLARE order_table VARCHAR(32);
  DECLARE item_table VARCHAR(32);

  WHILE database_index < 2 DO
    SET schema_name = CONCAT('ds_order_', database_index);
    SET table_index = 0;
    WHILE table_index < 16 DO
      SET order_table = CONCAT('t_order_', table_index);
      SET item_table = CONCAT('t_order_item_', table_index);

      IF EXISTS (SELECT 1 FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = order_table)
         AND NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                         WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = order_table
                           AND COLUMN_NAME = 'order_no' AND SEQ_IN_INDEX = 1) THEN
        SET @verify_index_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', order_table,
                                       '` ADD INDEX `idx_verify_order_no` (`order_no`)');
        PREPARE verify_index_stmt FROM @verify_index_sql;
        EXECUTE verify_index_stmt;
        DEALLOCATE PREPARE verify_index_stmt;
      END IF;

      IF EXISTS (SELECT 1 FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = item_table)
         AND NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                         WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = item_table
                           AND COLUMN_NAME = 'check_code' AND SEQ_IN_INDEX = 1) THEN
        SET @verify_index_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', item_table,
                                       '` ADD INDEX `idx_verify_check_code` (`check_code`)');
        PREPARE verify_index_stmt FROM @verify_index_sql;
        EXECUTE verify_index_stmt;
        DEALLOCATE PREPARE verify_index_stmt;
      END IF;

      SET table_index = table_index + 1;
    END WHILE;
    SET database_index = database_index + 1;
  END WHILE;
END$$
CALL upgrade_ticket_lookup_indexes()$$
DROP PROCEDURE upgrade_ticket_lookup_indexes$$
DELIMITER ;

-- END 14_verify_ticket_lookup_indexes.sql

-- ============================================================================
-- BEGIN 15_verify_ticket_records.sql
-- ============================================================================
-- Upgrade existing order item shards. Safe to rerun after 02_livestart_sharded_schema.sql.
USE `live_start`;
DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_ticket_verify_records$$
CREATE PROCEDURE upgrade_ticket_verify_records()
BEGIN
  DECLARE database_index INT DEFAULT 0;
  DECLARE table_index INT;
  DECLARE schema_name VARCHAR(32);
  DECLARE target_table VARCHAR(32);

  WHILE database_index < 2 DO
    SET schema_name = CONCAT('ds_order_', database_index);
    SET table_index = 0;
    WHILE table_index < 16 DO
      SET target_table = CONCAT('t_order_item_', table_index);
      IF EXISTS (SELECT 1 FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = target_table) THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = target_table
                         AND COLUMN_NAME = 'checked_at') THEN
          SET @verify_records_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', target_table,
                                           '` ADD COLUMN `checked_at` datetime DEFAULT NULL COMMENT ''成功核销时间''');
          PREPARE verify_records_stmt FROM @verify_records_sql;
          EXECUTE verify_records_stmt;
          DEALLOCATE PREPARE verify_records_stmt;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = target_table
                         AND COLUMN_NAME = 'checked_by') THEN
          SET @verify_records_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', target_table,
                                           '` ADD COLUMN `checked_by` bigint DEFAULT NULL COMMENT ''核销操作人用户ID''');
          PREPARE verify_records_stmt FROM @verify_records_sql;
          EXECUTE verify_records_stmt;
          DEALLOCATE PREPARE verify_records_stmt;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                       WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = target_table
                         AND COLUMN_NAME = 'checked_at' AND SEQ_IN_INDEX = 1) THEN
          SET @verify_records_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', target_table,
                                           '` ADD INDEX `idx_checked_at` (`checked_at`)');
          PREPARE verify_records_stmt FROM @verify_records_sql;
          EXECUTE verify_records_stmt;
          DEALLOCATE PREPARE verify_records_stmt;
        END IF;
      END IF;
      SET table_index = table_index + 1;
    END WHILE;
    SET database_index = database_index + 1;
  END WHILE;
END$$
CALL upgrade_ticket_verify_records()$$
DROP PROCEDURE upgrade_ticket_verify_records$$
DELIMITER ;

-- END 15_verify_ticket_records.sql

-- ============================================================================
-- BEGIN 16_settlement_notification_read.sql
-- ============================================================================
-- Existing databases: create the settlement notification state table before starting settlement.
USE `live_start`;

CREATE TABLE IF NOT EXISTS `t_settlement_notification_read` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '后台用户ID',
  `notification_key` varchar(120) NOT NULL COMMENT '通知唯一键',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_notification` (`user_id`, `notification_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算通知已读记录';

-- END 16_settlement_notification_read.sql

-- ============================================================================
-- BEGIN 17_artist_wallet_withdrawal.sql
-- ============================================================================
/*
 * 艺人推广收益钱包与提现表。
 * 执行顺序：09_distribution_tables_upgrade.sql 之后。
 * 本脚本只新增表，不删除历史数据。
 */
USE `live_start`;

CREATE TABLE IF NOT EXISTS `t_artist_wallet_account` (
  `artist_id` bigint NOT NULL COMMENT '艺人用户ID，关联 t_user.id',
  `available_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '可提现余额',
  `frozen_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '提现冻结金额',
  `total_earned` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计入账金额',
  `total_withdrawn` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计提现成功金额',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`artist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人推广收益钱包';

CREATE TABLE IF NOT EXISTS `t_artist_wallet_ledger` (
  `id` bigint NOT NULL COMMENT '账本流水ID',
  `artist_id` bigint NOT NULL COMMENT '艺人用户ID',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型：COMMISSION、WITHDRAW_FREEZE、WITHDRAW_RELEASE、WITHDRAW_PAID、REFUND',
  `biz_no` varchar(64) NOT NULL COMMENT '业务唯一号',
  `direction` tinyint NOT NULL COMMENT '方向 1:增加 2:减少',
  `amount` decimal(12,2) NOT NULL COMMENT '变动金额，正数',
  `balance_after` decimal(12,2) NOT NULL COMMENT '变动后可用余额',
  `remark` varchar(255) DEFAULT NULL COMMENT '流水说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artist_wallet_biz` (`artist_id`,`biz_type`,`biz_no`),
  KEY `idx_artist_wallet_time` (`artist_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人钱包不可变账本';

CREATE TABLE IF NOT EXISTS `t_artist_withdrawal` (
  `id` bigint NOT NULL COMMENT '提现申请ID',
  `request_no` varchar(64) NOT NULL COMMENT '提现请求幂等号',
  `artist_id` bigint NOT NULL COMMENT '艺人用户ID',
  `amount` decimal(12,2) NOT NULL COMMENT '提现金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0:待审核 1:处理中 2:已完成 3:已拒绝 4:已取消',
  `account_type` varchar(32) NOT NULL COMMENT '收款账户类型',
  `account_no` varchar(128) NOT NULL COMMENT '收款账号快照',
  `account_name` varchar(128) DEFAULT NULL COMMENT '收款人姓名快照',
  `external_no` varchar(128) DEFAULT NULL COMMENT '外部支付流水号',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artist_withdrawal_request` (`request_no`),
  KEY `idx_artist_withdrawal_status` (`artist_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人提现申请';

-- END 17_artist_wallet_withdrawal.sql

-- ============================================================================
-- BEGIN 07_pay_service_schema.sql
-- ============================================================================
DROP DATABASE IF EXISTS `live_start_pay`;
CREATE DATABASE `live_start_pay` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
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

CREATE TABLE IF NOT EXISTS `t_refund` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LiveStart退款单';

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付事件Outbox';

-- END 07_pay_service_schema.sql
