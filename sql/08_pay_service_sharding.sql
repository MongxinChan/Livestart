-- 启用 pay-sharding profile 前执行本脚本。
-- 每个数据库创建 16 张同构表；order_no 是支付域唯一且稳定的分片键。
CREATE DATABASE IF NOT EXISTS `live_start_pay_0` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `live_start_pay_1` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 先在两个库各执行一次 07_pay_service_schema.sql 中 t_pay/t_refund 的建表语句，
-- 再将模板复制为 t_pay_0..t_pay_15 和 t_refund_0..t_refund_15。
-- 生产环境建议使用 DBA 变更工具展开以下 32 张表，避免在线 DDL 误操作。
