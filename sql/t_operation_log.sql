/*
 Livestart 操作日志表 (统一管理)
 
 放在 ds_common 公共库中，通过 type 字段区分业务模块。
 适用于商户后台所有敏感操作的审计留痕。

 说明：
   - 系统体量为毕设级别，采用"统一管理"而非"细粒度拆分"策略
   - 操作日志属于低频写入（管理员后台操作），无需分表
   - type 字段用于区分不同业务模块：Event、TicketSku 等
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for t_operation_log
-- 统一操作审计日志表
-- ----------------------------
DROP TABLE IF EXISTS `t_operation_log`;
CREATE TABLE `t_operation_log` (
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `type`            varchar(64)  NOT NULL                COMMENT '操作类型 (如 Event, TicketSku)',
    `biz_no`          varchar(128) DEFAULT NULL            COMMENT '业务单号 (演出ID / 票种ID 等)',
    `operator_id`     varchar(64)  DEFAULT NULL            COMMENT '操作人ID',
    `operator_name`   varchar(128) DEFAULT NULL            COMMENT '操作人姓名',
    `operation_log`   text                                 COMMENT '操作日志描述',
    `original_data`   varchar(2048) DEFAULT NULL           COMMENT '原始数据 (JSON)',
    `modified_data`   varchar(2048) DEFAULT NULL           COMMENT '修改后数据 (JSON)',
    `create_time`     datetime     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`) USING BTREE,
    KEY `idx_biz_no` (`biz_no`) USING BTREE,
    KEY `idx_create_time` (`create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一操作审计日志表';

SET FOREIGN_KEY_CHECKS = 1;
