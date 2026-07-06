/*
 * LiveStart settlement compatibility upgrade.
 *
 * Purpose:
 *   - Keep existing live_start.t_settlement compatible with settlement service releases
 *     that write longer exception messages.
 *   - Upgrade error_message from varchar(500) or any smaller character type to TEXT.
 *   - Provide a paid-only settlement amount verification query for manual checks.
 *
 * Recommended usage order:
 *   1. 01_livestart_common_schema.sql
 *   2. 02_livestart_sharded_schema.sql
 *   3. 03_tables_xxl_job.sql
 *   4. 04_feature_and_job_upgrade.sql
 *   5. 07_settlement_error_message_text_and_paid_only.sql
 *   6. 05_test_seed_data.sql
 */

DELIMITER //

DROP PROCEDURE IF EXISTS `live_start`.`sp_upgrade_settlement_error_message_text`//
CREATE PROCEDURE `live_start`.`sp_upgrade_settlement_error_message_text`()
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'live_start'
      AND TABLE_NAME = 't_settlement'
  ) THEN
    IF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'live_start'
        AND TABLE_NAME = 't_settlement'
        AND COLUMN_NAME = 'error_message'
    ) THEN
      ALTER TABLE `live_start`.`t_settlement`
        ADD COLUMN `error_message` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结算异常信息'
        AFTER `status`;
    ELSEIF NOT EXISTS (
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = 'live_start'
        AND TABLE_NAME = 't_settlement'
        AND COLUMN_NAME = 'error_message'
        AND DATA_TYPE IN ('text', 'mediumtext', 'longtext')
    ) THEN
      ALTER TABLE `live_start`.`t_settlement`
        MODIFY COLUMN `error_message` text COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结算异常信息';
    END IF;
  END IF;
END//

DELIMITER ;

CALL `live_start`.`sp_upgrade_settlement_error_message_text`();
DROP PROCEDURE IF EXISTS `live_start`.`sp_upgrade_settlement_error_message_text`;

SELECT
  TABLE_SCHEMA,
  TABLE_NAME,
  COLUMN_NAME,
  DATA_TYPE,
  CHARACTER_MAXIMUM_LENGTH
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'live_start'
  AND TABLE_NAME = 't_settlement'
  AND COLUMN_NAME = 'error_message';

/*
 * Paid-only manual verification template.
 *
 * Set @settlement_event_id to the event id being checked before running this
 * query. The settlement service should count only o.status = 1. Orders in
 * status 0, 2, or 3 must not contribute tickets or amount to settlement.
 */
SET @settlement_event_id = 0;

SELECT
  oi.event_id,
  COUNT(*) AS paid_ticket_count,
  SUM(sku.selling_price) AS paid_sales_amount,
  ROUND(SUM(sku.selling_price) * 0.0500, 2) AS commission_amount,
  ROUND(SUM(sku.selling_price) - ROUND(SUM(sku.selling_price) * 0.0500, 2), 2) AS settlement_amount
FROM (
  SELECT * FROM `ds_order_0`.`t_order_item_0`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_1`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_2`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_3`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_4`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_5`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_6`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_7`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_8`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_9`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_10`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_11`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_12`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_13`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_14`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_item_15`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_0`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_1`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_2`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_3`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_4`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_5`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_6`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_7`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_8`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_9`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_10`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_11`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_12`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_13`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_14`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_item_15`
) oi
JOIN (
  SELECT * FROM `ds_order_0`.`t_order_0`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_1`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_2`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_3`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_4`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_5`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_6`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_7`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_8`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_9`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_10`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_11`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_12`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_13`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_14`
  UNION ALL SELECT * FROM `ds_order_0`.`t_order_15`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_0`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_1`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_2`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_3`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_4`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_5`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_6`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_7`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_8`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_9`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_10`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_11`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_12`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_13`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_14`
  UNION ALL SELECT * FROM `ds_order_1`.`t_order_15`
) o ON o.order_no = oi.order_no
JOIN `live_start`.`t_ticket_sku` sku ON sku.id = oi.sku_id
WHERE oi.event_id = @settlement_event_id
  AND o.status = 1
GROUP BY oi.event_id;
