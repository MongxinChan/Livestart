/*
 Livestart 用户分片数据库 (ds_user_x) 建表脚本模板
 
 主要职责：
 1. 实现用户域数据水平拆分，由 ds_user_0, ds_user_1 两个物理库分摊存储及吞吐压力。
 2. 每个物理库包含 8 张分片物理表 (t_user_0 ~ t_user_7, t_user_profile_0 ~ t_user_profile_7, t_user_visitor_0 ~ t_user_visitor_7)。
 3. 分片键：user_id (即主表的 id，关联表的 user_id)。
 4. 广播配置表：同步更新复制，方便在同一数据库节点进行只读关联。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================================
-- 1. 广播表/只读元数据副本（每个物理分片库均同步存在一份，由中间件广播双写）
-- =========================================================================

-- ----------------------------
-- Table structure for t_event
-- ----------------------------
DROP TABLE IF EXISTS `t_event`;
CREATE TABLE `t_event` (
  `id` bigint NOT NULL COMMENT '演出主键ID(分布式ID)',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '演出标题',
  `event_type` tinyint(1) NOT NULL COMMENT '演出类型 0:Livehouse 1:演唱会',
  `venue_id` bigint NOT NULL COMMENT '关联场馆ID',
  `start_time` datetime NOT NULL COMMENT '演出开始时间',
  `poster_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '海报图片地址',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态 0:下架 1:预售 2:在售 3:售罄',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出基础信息表(广播表)' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_event_config
-- ----------------------------
DROP TABLE IF EXISTS `t_event_config`;
CREATE TABLE `t_event_config` (
  `event_id` bigint NOT NULL COMMENT '对应演出ID',
  `selection_mode` tinyint(1) NOT NULL DEFAULT 0 COMMENT '选座模式 0:系统自动配座 1:手动选座',
  `is_verify_required` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否强制实名制入场 0:否 1:是',
  `max_tickets_per_user` int NULL DEFAULT 6 COMMENT '单人账户最大购票上限',
  `refund_policy_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '退票政策 0:不可退 1:全额退 2:阶梯退票',
  `tier1_free_refund_hours` int NULL DEFAULT NULL COMMENT '全额退票截止时间',
  `tier2_partial_refund_hours` int NULL DEFAULT NULL COMMENT '部分退票截止时间',
  `tier2_refund_fee_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '部分退票手续费比例',
  `is_transferable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否允许转赠门票 0:否 1:是',
  `is_waiting_allowed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启候补购票功能',
  PRIMARY KEY (`event_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出配置表(广播表)' ROW_FORMAT = Dynamic;


-- =========================================================================
-- 2. 用户分片物理表结构定义 (t_user_0 ~ t_user_7)
-- =========================================================================

-- 产生 8 组分片表
-- 为了在初始化数据库时能一键导入，以下展开编写 8 张物理分片表的 DDL。

-- ----------------------------
-- 物理分表: t_user_{0..7}
-- ----------------------------
DROP TABLE IF EXISTS `t_user_0`; CREATE TABLE `t_user_0` like `t_user_template`;
DROP TABLE IF EXISTS `t_user_1`; CREATE TABLE `t_user_1` like `t_user_template`;
DROP TABLE IF EXISTS `t_user_2`; CREATE TABLE `t_user_2` like `t_user_template`;
DROP TABLE IF EXISTS `t_user_3`; CREATE TABLE `t_user_3` like `t_user_template`;
DROP TABLE IF EXISTS `t_user_4`; CREATE TABLE `t_user_4` like `t_user_template`;
DROP TABLE IF EXISTS `t_user_5`; CREATE TABLE `t_user_5` like `t_user_template`;
DROP TABLE IF EXISTS `t_user_6`; CREATE TABLE `t_user_6` like `t_user_template`;
DROP TABLE IF EXISTS `t_user_7`; CREATE TABLE `t_user_7` like `t_user_template`;

-- ----------------------------
-- 物理分表: t_user_profile_{0..7}
-- ----------------------------
DROP TABLE IF EXISTS `t_user_profile_0`; CREATE TABLE `t_user_profile_0` like `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_profile_1`; CREATE TABLE `t_user_profile_1` like `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_profile_2`; CREATE TABLE `t_user_profile_2` like `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_profile_3`; CREATE TABLE `t_user_profile_3` like `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_profile_4`; CREATE TABLE `t_user_profile_4` like `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_profile_5`; CREATE TABLE `t_user_profile_5` like `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_profile_6`; CREATE TABLE `t_user_profile_6` like `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_profile_7`; CREATE TABLE `t_user_profile_7` like `t_user_profile_template`;

-- ----------------------------
-- 物理分表: t_user_visitor_{0..7}
-- ----------------------------
DROP TABLE IF EXISTS `t_user_visitor_0`; CREATE TABLE `t_user_visitor_0` like `t_user_visitor_template`;
DROP TABLE IF EXISTS `t_user_visitor_1`; CREATE TABLE `t_user_visitor_1` like `t_user_visitor_template`;
DROP TABLE IF EXISTS `t_user_visitor_2`; CREATE TABLE `t_user_visitor_2` like `t_user_visitor_template`;
DROP TABLE IF EXISTS `t_user_visitor_3`; CREATE TABLE `t_user_visitor_3` like `t_user_visitor_template`;
DROP TABLE IF EXISTS `t_user_visitor_4`; CREATE TABLE `t_user_visitor_4` like `t_user_visitor_template`;
DROP TABLE IF EXISTS `t_user_visitor_5`; CREATE TABLE `t_user_visitor_5` like `t_user_visitor_template`;
DROP TABLE IF EXISTS `t_user_visitor_6`; CREATE TABLE `t_user_visitor_6` like `t_user_visitor_template`;
DROP TABLE IF EXISTS `t_user_visitor_7`; CREATE TABLE `t_user_visitor_7` like `t_user_visitor_template`;

-- =========================================================================
-- 附: 基础物理表模板结构定义 (仅用于结构声明，可直接运行)
-- =========================================================================

-- ----------------------------
-- Template Structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user_template`;
CREATE TABLE `t_user_template` (
  `id` bigint NOT NULL COMMENT '用户ID(分布式ID，Snowflake)',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '加密存储的密码',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号(唯一登录凭证)',
  `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号(AES加密存储)',
  `is_verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否实名认证 0:否 1:是',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名(实名认证后写入)',
  `user_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '用户类型 1:乐迷 2:艺人 3:主办方 4:管理员',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '账号状态 1:正常 0:禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_time` datetime NULL DEFAULT NULL COMMENT '注销/删除时间',
  `del_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'C端用户主表模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Template Structure for t_user_profile
-- ----------------------------
DROP TABLE IF EXISTS `t_user_profile_template`;
CREATE TABLE `t_user_profile_template` (
  `user_id` bigint NOT NULL COMMENT '关联 t_user.id',
  `mail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `gender` tinyint(1) NULL DEFAULT 0,
  `signature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `birthday` date NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户社交资料表模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Template Structure for t_user_visitor
-- ----------------------------
DROP TABLE IF EXISTS `t_user_visitor_template`;
CREATE TABLE `t_user_visitor_template` (
  `id` bigint NOT NULL COMMENT '观演人ID(分布式ID，Snowflake)',
  `user_id` bigint NOT NULL COMMENT '所属用户ID (关联 t_user.id)',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '观演人真实姓名',
  `card_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '证件类型 1:身份证 2:护照 3:港澳通行证 4:台胞证',
  `card_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证件号码 (必须AES加密存储)',
  `card_no_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证件号哈希值 (用于判重)',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '观演人手机号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `del_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0:未删 1:已删',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_card`(`user_id` ASC, `card_no_hash` ASC, `del_flag` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '常用观演人表模板' ROW_FORMAT = DYNAMIC;

-- 执行实际表克隆以生成具体分片表
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

-- 移除模板表，仅留分片表，防止污染结构（按需保留或删除）
DROP TABLE IF EXISTS `t_user_template`;
DROP TABLE IF EXISTS `t_user_profile_template`;
DROP TABLE IF EXISTS `t_user_visitor_template`;

SET FOREIGN_KEY_CHECKS = 1;
