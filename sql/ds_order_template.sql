/*
 Livestart 订单分片数据库 (ds_order_x) 建表脚本模板
 
 主要职责：
 1. 实现高并发交易下的订单域水平拆分，分摊支付回调、出票、退票的高频 I/O 读写。
 2. 物理库包含 ds_order_0, ds_order_1，每个库各含 16 张物理表 (t_order_0 ~ t_order_15, t_order_item_0 ~ t_order_item_15)。
 3. 分片键：user_id (主表 t_order 和明细表 t_order_item 共同使用 user_id，形成 Binding Table 绑定表关系，消除跨库跨表 Join 损耗)。
 4. 在 t_order_item 中，已显式新增 user_id 字段以适配 Binding Table 设计。
 5. 广播表：为了避免在创建订单和算价时跨库 RPC 查询演出和票种，在订单库中同步保留只读副本广播表。
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

-- ----------------------------
-- Table structure for ticket_skus
-- ----------------------------
DROP TABLE IF EXISTS `ticket_skus`;
CREATE TABLE `ticket_skus` (
  `id` bigint NOT NULL COMMENT '票档主键ID(分布式ID)',
  `event_id` bigint NOT NULL COMMENT '关联演出ID',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '票种名称',
  `original_price` decimal(10, 2) NOT NULL COMMENT '原价',
  `selling_price` decimal(10, 2) NOT NULL COMMENT '售价',
  `total_stock` int NOT NULL COMMENT '总库存',
  `remaining_stock` int NOT NULL COMMENT '当前剩余库存',
  `limit_num` int NULL DEFAULT 6 COMMENT '单人限购数量',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本号(高并发核心)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_id`(`event_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出票档库存表(广播表)' ROW_FORMAT = Dynamic;


-- =========================================================================
-- 2. 订单分片物理表结构定义 (t_order_0 ~ t_order_15)
-- =========================================================================

-- ----------------------------
-- 物理分表: t_order_{0..15}
-- ----------------------------
DROP TABLE IF EXISTS `t_order_0`; CREATE TABLE `t_order_0` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_1`; CREATE TABLE `t_order_1` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_2`; CREATE TABLE `t_order_2` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_3`; CREATE TABLE `t_order_3` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_4`; CREATE TABLE `t_order_4` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_5`; CREATE TABLE `t_order_5` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_6`; CREATE TABLE `t_order_6` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_7`; CREATE TABLE `t_order_7` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_8`; CREATE TABLE `t_order_8` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_9`; CREATE TABLE `t_order_9` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_10`; CREATE TABLE `t_order_10` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_11`; CREATE TABLE `t_order_11` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_12`; CREATE TABLE `t_order_12` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_13`; CREATE TABLE `t_order_13` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_14`; CREATE TABLE `t_order_14` like `t_order_template`;
DROP TABLE IF EXISTS `t_order_15`; CREATE TABLE `t_order_15` like `t_order_template`;

-- ----------------------------
-- 物理分表: t_order_item_{0..15}
-- ----------------------------
DROP TABLE IF EXISTS `t_order_item_0`; CREATE TABLE `t_order_item_0` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_1`; CREATE TABLE `t_order_item_1` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_2`; CREATE TABLE `t_order_item_2` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_3`; CREATE TABLE `t_order_item_3` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_4`; CREATE TABLE `t_order_item_4` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_5`; CREATE TABLE `t_order_item_5` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_6`; CREATE TABLE `t_order_item_6` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_7`; CREATE TABLE `t_order_item_7` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_8`; CREATE TABLE `t_order_item_8` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_9`; CREATE TABLE `t_order_item_9` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_10`; CREATE TABLE `t_order_item_10` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_11`; CREATE TABLE `t_order_item_11` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_12`; CREATE TABLE `t_order_item_12` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_13`; CREATE TABLE `t_order_item_13` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_14`; CREATE TABLE `t_order_item_14` like `t_order_item_template`;
DROP TABLE IF EXISTS `t_order_item_15`; CREATE TABLE `t_order_item_15` like `t_order_item_template`;

-- =========================================================================
-- 附: 基础物理表模板结构定义 (仅用于结构声明，可直接运行)
-- =========================================================================

-- ----------------------------
-- Template Structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order_template`;
CREATE TABLE `t_order_template` (
  `id` bigint NOT NULL COMMENT '订单ID(分布式唯一雪花ID)',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单流水号(采用基因算法路由)',
  `user_id` bigint NOT NULL COMMENT '下单用户ID(Sharding Key)',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单实付总额',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态 0:待支付 1:已支付 2:已核销 3:已取消',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单主表模板' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Template Structure for t_order_item
-- ----------------------------
DROP TABLE IF EXISTS `t_order_item_template`;
CREATE TABLE `t_order_item_template` (
  `id` bigint NOT NULL COMMENT '电子票/明细ID(分布式唯一雪花ID)',
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联订单号',
  `user_id` bigint NOT NULL COMMENT '下单用户ID(Sharding Key - 新增以实现Binding Table)',
  `visitor_id` bigint NOT NULL COMMENT '关联实际观演人身份证信息',
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `sku_id` bigint NOT NULL COMMENT '票档ID',
  `seat_id` bigint NULL DEFAULT NULL COMMENT '关联座位ID(如选座)',
  `check_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一核销码数据',
  `is_checked` tinyint(1) NULL DEFAULT 0 COMMENT '核销状态 0:未入场 1:已入场',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_check_code`(`check_code` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单明细/电子票表模板' ROW_FORMAT = Dynamic;


-- 执行实际表克隆以生成具体分片表
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

-- 移除模板表，仅留分片表
DROP TABLE IF EXISTS `t_order_template`;
DROP TABLE IF EXISTS `t_order_item_template`;

SET FOREIGN_KEY_CHECKS = 1;
