/*
 Livestart 公共元配置数据库 (ds_common) 建表脚本
 
 主要职责：
 1. 存储低频、静态或全局配置类元数据：场馆、艺人、风格、关联关系、评论。
 2. 存储全局映射路由表：手机号登录态映射路由。
 
 实体主键生成策略说明：
 统一废除 MySQL 物理 AUTO_INCREMENT（自增主键），改为使用分布式全局唯一 ID（雪花算法 Snowflake）机制，由应用层（或数据网关中间件）生成后写入。
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_user_phone_mapping
-- 账号手机号与用户ID全局路由映射表（高并发登录精准路由防全表扫描核心设计）
-- ----------------------------
DROP TABLE IF EXISTS `t_user_phone_mapping`;
CREATE TABLE `t_user_phone_mapping` (
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '手机号(唯一索引 & 登录凭证)',
  `user_id` bigint NOT NULL COMMENT '关联分布式用户ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`phone`) USING BTREE,
  UNIQUE INDEX `idx_unique_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户手机号与全局ID映射路由表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_venue
-- 场馆基础信息表（作为全局单表保存）
-- ----------------------------
DROP TABLE IF EXISTS `t_venue`;
CREATE TABLE `t_venue` (
  `id` bigint NOT NULL COMMENT '场馆主键ID(分布式唯一ID)',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '场馆名称',
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '城市',
  `address` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '详细地址',
  `capacity` int NULL DEFAULT NULL COMMENT '场馆总容纳人数',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '场馆信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_performer
-- 艺人/乐队信息表（作为全局单表保存）
-- ----------------------------
DROP TABLE IF EXISTS `t_performer`;
CREATE TABLE `t_performer` (
  `id` bigint NOT NULL COMMENT '艺人/乐队主键ID(分布式唯一ID)',
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '艺人/乐队名称',
  `style_id` bigint NULL DEFAULT NULL COMMENT '关联主打风格ID',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '艺人头像/Logo URL',
  `bio` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '艺人/乐队详细介绍',
  `status` tinyint(1) NOT NULL DEFAULT 1 COMMENT '状态 1:正常 0:停演',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_style_id`(`style_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '艺人/乐队信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_style
-- 音乐风格定义表（全局单表）
-- ----------------------------
DROP TABLE IF EXISTS `t_style`;
CREATE TABLE `t_style` (
  `id` bigint NOT NULL COMMENT '风格主键ID(分布式唯一ID)',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '风格名称',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '风格代码',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '风格描述',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_unique_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '音乐风格定义表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_performer_style_relation
-- 艺人风格关联多对多中间表
-- ----------------------------
DROP TABLE IF EXISTS `t_performer_style_relation`;
CREATE TABLE `t_performer_style_relation` (
  `performer_id` bigint NOT NULL COMMENT '艺人ID',
  `style_id` bigint NOT NULL COMMENT '风格ID',
  PRIMARY KEY (`performer_id`, `style_id`) USING BTREE,
  INDEX `idx_style_performer`(`style_id` ASC, `performer_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '艺人风格关联中间表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for t_comment
-- 演出评论与回复表（MVP阶段暂作为普通全局表，不进行分片）
-- ----------------------------
DROP TABLE IF EXISTS `t_comment`;
CREATE TABLE `t_comment` (
  `id` bigint NOT NULL COMMENT '评论主键ID(分布式唯一ID)',
  `event_id` bigint NOT NULL COMMENT '演出ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `status` tinyint NULL DEFAULT 0 COMMENT '审核状态 0:待审 1:通过 2:屏蔽',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_event_id`(`event_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评论回复表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
