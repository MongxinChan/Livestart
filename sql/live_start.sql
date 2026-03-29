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

 Date: 29/03/2026 11:55:56
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_comment
-- ----------------------------
DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `status` tinyint NULL DEFAULT 0 COMMENT '审核状态 0:待审 1:通过 2:屏蔽',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评论回复表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_event
-- ----------------------------
DROP TABLE IF EXISTS `t_event`;
CREATE TABLE `t_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '演出标题',
  `event_type` tinyint(1) NOT NULL COMMENT '演出类型 0:Livehouse(站票) 1:演唱会(选座/ABCD区)',
  `venue_id` bigint NOT NULL COMMENT '关联场馆ID',
  `start_time` datetime NOT NULL COMMENT '演出开始时间',
  `poster_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '海报图片地址',
  `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态 0:下架 1:预售 2:在售 3:售罄',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出基础信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_event_config
-- ----------------------------
DROP TABLE IF EXISTS `t_event_config`;
CREATE TABLE `t_event_config`  (
  `event_id` bigint NOT NULL COMMENT '对应演出ID',
  `selection_mode` tinyint(1) NOT NULL DEFAULT 0 COMMENT '选座模式 0:系统自动配座(高并发演唱会) 1:手动选座(音乐剧/剧场)',
  `is_verify_required` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否强制实名制入场 0:否 1:是',
  `max_tickets_per_user` int NULL DEFAULT 6 COMMENT '单人账户最大购票上限',
  `refund_policy_type` tinyint(1) NOT NULL DEFAULT 0 COMMENT '退票政策 0:不可退 1:全额退 2:阶梯退票',
  `tier1_free_refund_hours` int NULL DEFAULT NULL COMMENT '全额退票截止时间(开演前X小时)',
  `tier2_partial_refund_hours` int NULL DEFAULT NULL COMMENT '部分退票截止时间(开演前Y小时)',
  `tier2_refund_fee_rate` decimal(5, 2) NULL DEFAULT 0.00 COMMENT '部分退票手续费比例 (0.20 代表扣除20%手续费)',
  `is_transferable` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否允许转赠门票 0:否 1:是',
  `is_waiting_allowed` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否开启候补购票功能',
  PRIMARY KEY (`event_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出业务配置及退票策略表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单流水号',
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `total_amount` decimal(10, 2) NOT NULL COMMENT '订单实付总额',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态 0:待支付 1:已支付 2:已核销 3:已取消',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '支付完成时间',
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
  `visitor_id` bigint NOT NULL COMMENT '关联实际观演人身份证信息',
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `sku_id` bigint NOT NULL COMMENT '票档ID',
  `seat_id` bigint NULL DEFAULT NULL COMMENT '关联座位ID(演唱会必填，Livehouse为空)',
  `check_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '唯一核销码数据(生成二维码用)',
  `is_checked` tinyint(1) NULL DEFAULT 0 COMMENT '核销状态 0:未入场 1:已入场',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_unique_check_code`(`check_code` ASC) USING BTREE,
  INDEX `idx_order_no`(`order_no` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '订单明细/电子票表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_performer
-- ----------------------------
DROP TABLE IF EXISTS `t_performer`;
CREATE TABLE `t_performer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '艺人/乐队名称',
  `style_id` bigint NULL DEFAULT NULL COMMENT '关联风格ID (关联 t_style.id)',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '艺人头像/Logo URL',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '艺人/乐队详细介绍',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 1:正常 0:停演',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_style_id`(`style_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '艺人/乐队信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_performer_style_relation
-- ----------------------------
DROP TABLE IF EXISTS `t_performer_style_relation`;
CREATE TABLE `t_performer_style_relation`  (
  `performer_id` bigint NOT NULL COMMENT '艺人ID',
  `style_id` bigint NOT NULL COMMENT '风格ID',
  PRIMARY KEY (`performer_id`, `style_id`) USING BTREE,
  INDEX `idx_style_performer`(`style_id` ASC, `performer_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '艺人风格关联中间表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_refund_policy
-- ----------------------------
DROP TABLE IF EXISTS `t_refund_policy`;
CREATE TABLE `t_refund_policy`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL COMMENT '关联演出ID',
  `is_allow_refund` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否允许退票 0:全场不可退 1:允许退票',
  `tier1_deadline_hours` int NULL DEFAULT 48 COMMENT '全额退截止时间(距离开演前X小时)',
  `tier2_deadline_hours` int NULL DEFAULT 24 COMMENT '部分退截止时间(距离开演前Y小时)',
  `tier2_refund_fee_rate` decimal(5, 2) NULL DEFAULT 0.20 COMMENT '部分退手续费比例 (0.20 代表扣除实付金额的20%)',
  `policy_desc` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '退票政策文案描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_event_id`(`event_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '阶梯退票费率配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_seat
-- ----------------------------
DROP TABLE IF EXISTS `t_seat`;
CREATE TABLE `t_seat`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL COMMENT '关联演出ID',
  `sku_id` bigint NOT NULL COMMENT '关联票档ID',
  `section` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '区域: A区/B区/C区/D区',
  `row_num` int NOT NULL COMMENT '排号 (用于连坐匹配)',
  `col_num` int NOT NULL COMMENT '列号',
  `status` tinyint(1) NOT NULL DEFAULT 0 COMMENT '状态 0:可选 1:锁定中 2:已售出',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_sku_section`(`event_id` ASC, `sku_id` ASC, `section` ASC) USING BTREE,
  INDEX `idx_row_col`(`row_num` ASC, `col_num` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演唱会物理座位详情表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_style
-- ----------------------------
DROP TABLE IF EXISTS `t_style`;
CREATE TABLE `t_style`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '风格名称 (如: 摇滚、民谣、流行、电子)',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '风格代码 (用于后端逻辑标识，如 ROCK, FOLK)',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '风格描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_unique_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '音乐风格定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '加密存储的密码',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号(唯一登录凭证)',
  `mail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `id_card` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '身份证号(AES加密存储)',
  `is_verified` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否实名认证 0:否 1:是',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '真实姓名(实名认证后写入)',
  `user_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '用户类型 1:乐迷 2:艺人 3:主办方 4:管理员',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '账号状态 1:正常 0:禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `delete_time` datetime NULL DEFAULT NULL COMMENT '注销/删除时间',
  `del_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '用户头像URL',
  `gender` tinyint(1) NULL DEFAULT 0 COMMENT '性别 0:保密 1:男 2:女',
  `signature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '个性签名',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_unique_phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'C端用户主表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user_profile
-- ----------------------------
DROP TABLE IF EXISTS `t_user_profile`;
CREATE TABLE `t_user_profile`  (
  `user_id` bigint NOT NULL COMMENT '关联 t_user.id',
  `mail` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `gender` tinyint(1) NULL DEFAULT 0,
  `signature` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `birthday` date NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户社交资料表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_user_visitor
-- ----------------------------
DROP TABLE IF EXISTS `t_user_visitor`;
CREATE TABLE `t_user_visitor`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID (关联 t_user.id)',
  `real_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '观演人真实姓名',
  `card_type` tinyint(1) NOT NULL DEFAULT 1 COMMENT '证件类型 1:身份证 2:护照 3:港澳通行证 4:台胞证',
  `card_no` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证件号码 (必须AES加密存储)',
  `card_no_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '证件号哈希值 (用于判重)',
  `mobile` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '观演人手机号(部分演出需要通知到人)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `del_flag` tinyint(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除 0:未删 1:已删',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_user_card`(`user_id` ASC, `card_no_hash` ASC, `del_flag` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '常用观演人表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for t_venue
-- ----------------------------
DROP TABLE IF EXISTS `t_venue`;
CREATE TABLE `t_venue`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场馆名称',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '城市',
  `address` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细地址',
  `capacity` int NULL DEFAULT NULL COMMENT '场馆总容纳人数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场馆信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ticket_skus
-- ----------------------------
DROP TABLE IF EXISTS `ticket_skus`;
CREATE TABLE `ticket_skus`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` bigint NOT NULL COMMENT '关联演出ID',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '票种名称: 如 680元档/VIP区/早鸟',
  `original_price` decimal(10, 2) NOT NULL COMMENT '原价',
  `selling_price` decimal(10, 2) NOT NULL COMMENT '售价',
  `total_stock` int NOT NULL COMMENT '总库存',
  `remaining_stock` int NOT NULL COMMENT '当前剩余库存',
  `limit_num` int NULL DEFAULT 6 COMMENT '单人限购数量',
  `version` int NULL DEFAULT 0 COMMENT '乐观锁版本号(高并发核心)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_id`(`event_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '演出票档库存表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
