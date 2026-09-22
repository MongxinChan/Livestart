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
