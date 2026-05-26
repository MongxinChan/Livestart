/*
 Livestart 座位与票务分片数据库 (ds_seat_x) 建表脚本模板
 
 主要职责：
 1. 实现演唱会等高并发下数以十万计的物理座位预锁定、状态更新的水平拆分，分摊 I/O。
 2. 物理库包含 ds_seat_0, ds_seat_1，每个库各含 8 张物理表 (t_seat_0 ~ t_seat_7)。
 3. 分片键：event_id (即演出主键ID)。保证单场演出的全部物理座位落在同一个库表中，极大优化锁座与连坐算法性能，消除跨库跨表数据整合开销。
 4. 广播表：同步保留演出票种 (ticket_skus) 与演出 (t_event)，用以本地关联校验。
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
-- 2. 座位分片物理表结构定义 (t_seat_0 ~ t_seat_7)
-- =========================================================================

-- ----------------------------
-- 物理分表: t_seat_{0..7}
-- ----------------------------
DROP TABLE IF EXISTS `t_seat_0`; CREATE TABLE `t_seat_0` like `t_seat_template`;
DROP TABLE IF EXISTS `t_seat_1`; CREATE TABLE `t_seat_1` like `t_seat_template`;
DROP TABLE IF EXISTS `t_seat_2`; CREATE TABLE `t_seat_2` like `t_seat_template`;
DROP TABLE IF EXISTS `t_seat_3`; CREATE TABLE `t_seat_3` like `t_seat_template`;
DROP TABLE IF EXISTS `t_seat_4`; CREATE TABLE `t_seat_4` like `t_seat_template`;
DROP TABLE IF EXISTS `t_seat_5`; CREATE TABLE `t_seat_5` like `t_seat_template`;
DROP TABLE IF EXISTS `t_seat_6`; CREATE TABLE `t_seat_6` like `t_seat_template`;
DROP TABLE IF EXISTS `t_seat_7`; CREATE TABLE `t_seat_7` like `t_seat_template`;

-- =========================================================================
-- 附: 基础物理表模板结构定义 (仅用于结构声明，可直接运行)
-- =========================================================================

-- ----------------------------
-- Template Structure for t_seat
-- ----------------------------
DROP TABLE IF EXISTS `t_seat_template`;
CREATE TABLE `t_seat_template` (
  `id` bigint NOT NULL COMMENT '座位ID(分布式唯一雪花ID)',
  `event_id` bigint NOT NULL COMMENT '关联演出ID(Sharding Key)',
  `sku_id` bigint NOT NULL COMMENT '关联票档ID',
  `section` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区域: 如 A区/B区/C区',
  `row_num` int NOT NULL COMMENT '排号 (用于连坐匹配)',
  `col_num` int NOT NULL COMMENT '列号',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态 0:可选 1:锁定中 2:已售出',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_sku_section`(`event_id` ASC, `sku_id` ASC, `section` ASC) USING BTREE,
  INDEX `idx_row_col`(`row_num` ASC, `col_num` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演唱会物理座位详情表模板' ROW_FORMAT = Dynamic;


-- 执行实际表克隆以生成具体分片表
CREATE TABLE `t_seat_0` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_1` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_2` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_3` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_4` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_5` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_6` SELECT * FROM `t_seat_template` WHERE 1=0;
CREATE TABLE `t_seat_7` SELECT * FROM `t_seat_template` WHERE 1=0;

-- 移除模板表，仅留分片表
DROP TABLE IF EXISTS `t_seat_template`;

SET FOREIGN_KEY_CHECKS = 1;
