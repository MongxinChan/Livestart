/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80032 (8.0.32)
 Source Host           : localhost:3306
 Source Schema         : live_start

 Target Server Type    : MySQL
 Target Server Version : 80032 (8.0.32)
 File Encoding         : 65001

 Date: 23/03/2026 23:21:52
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_event
-- ----------------------------
DROP TABLE IF EXISTS `t_event`;
CREATE TABLE `t_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '演出标题',
  `event_type` tinyint NOT NULL COMMENT '类型 0:Livehouse(站票) 1:演唱会(选座)',
  `venue_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场馆名称',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '城市',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态 0:下架 1:预售 2:在售 3:售罄',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出基础信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_event_config
-- ----------------------------
DROP TABLE IF EXISTS `t_event_config`;
CREATE TABLE `t_event_config`  (
  `event_id` bigint NOT NULL,
  `selection_mode` tinyint NOT NULL DEFAULT 0 COMMENT '0:系统自动配座 1:手动选座',
  `max_tickets_per_user` int NULL DEFAULT 6 COMMENT '单人限购上限',
  `tier1_free_refund_hours` int NULL DEFAULT 48 COMMENT '全额退截止时间(开演前X小时)',
  `tier2_partial_refund_hours` int NULL DEFAULT 24 COMMENT '部分退截止时间(开演前Y小时)',
  `tier2_refund_fee_rate` decimal(5, 2) NULL DEFAULT 0.20 COMMENT '部分退手续费比例(0.2代表20%)',
  PRIMARY KEY (`event_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出业务及退票策略表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单流水号',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '实付款金额',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0:待付 1:已付 2:已核销 3:已取消',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_unique_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_order_item
-- ----------------------------
DROP TABLE IF EXISTS `t_order_item`;
CREATE TABLE `t_order_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '关联订单号',
  `visitor_id` bigint NOT NULL COMMENT '关联观演人',
  `event_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `seat_id` bigint NULL DEFAULT NULL COMMENT '关联座位(演唱会必填)',
  `check_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一核销码数据',
  `is_checked` tinyint(1) NULL DEFAULT 0 COMMENT '是否已核销',
  `refund_fee` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '退票扣除的手续费',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_check_code`(`check_code` ASC) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单明细及电子票表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_seat
-- ----------------------------
DROP TABLE IF EXISTS `t_seat`;
CREATE TABLE `t_seat`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `sku_id` bigint NOT NULL,
  `section` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区域: A区/B区/看台',
  `row_num` int NOT NULL COMMENT '排号',
  `col_num` int NOT NULL COMMENT '座号',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0:空闲 1:锁定 2:已售',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_sku_row`(`event_id` ASC, `sku_id` ASC, `section` ASC, `row_num` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演唱会物理座位表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称/用户名',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '密码(加密存储)',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号(唯一登录凭证)',
  `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号(必须AES加密存储,脱敏显示)',
  `is_verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否实名认证 0:否 1:是',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像URL',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '账号状态 1:正常 0:禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_unique_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'C端用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user_visitor
-- ----------------------------
DROP TABLE IF EXISTS `t_user_visitor`;
CREATE TABLE `t_user_visitor`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID(关联t_user.id)',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '观演人真实姓名',
  `card_type` tinyint NOT NULL DEFAULT 1 COMMENT '证件类型 1:身份证 2:护照 3:港澳 4:台胞',
  `card_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证件号码(必须AES加密存储)',
  `card_no_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证件号哈希值(用于高并发判重)',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '观演人手机号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_card`(`user_id` ASC, `card_no_hash` ASC, `del_flag` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '常用观演人表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ticket_skus
-- ----------------------------
DROP TABLE IF EXISTS `ticket_skus`;
CREATE TABLE `ticket_skus`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '票种名称: 早鸟/预售/1280档',
  `original_price` decimal(10, 2) NOT NULL COMMENT '原价',
  `selling_price` decimal(10, 2) NOT NULL COMMENT '售价',
  `total_stock` int NOT NULL COMMENT '总库存',
  `remaining_stock` int NOT NULL COMMENT '剩余库存',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_id`(`event_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出票档库存表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
