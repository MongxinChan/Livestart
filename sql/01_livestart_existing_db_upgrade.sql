/*
 * LiveStart existing-database upgrade bundle.
 *
 * Use this file only for databases that already contain LiveStart data.
 * Review the migration plans and stop writes before executing the procedures
 * from 06, 11, and 12. The remaining sections apply idempotent schema fixes.
 * Do not run this file after 00_livestart_full_schema.sql on a new database.
 */

SET NAMES utf8mb4;

-- ============================================================================
-- BEGIN 06_migrate_order_shards_hash16_to_hash32.sql
-- ============================================================================
/*
 =========================================================================
 LiveStart order shard migration: old half-used layout -> current 32 physical shards
 =========================================================================

 Current target route (32 physical shards = 2 databases * 16 tables):
   databaseIndex = positiveJavaLongHash(user_id) % 2
   tableIndex    = (positiveJavaLongHash(user_id) / 2) % 16

 Historical data may have been written by direct user_id modulo or by the
 older hash % 16 table rule, so the procedure recalculates the target location
 from the current route instead of assuming same-database movement.

 Usage:
   1. Dry-run plan only:
        CALL live_start.sp_migrate_order_shards_hash16_to_hash32(FALSE);

   2. Execute after checking the plan:
        CALL live_start.sp_migrate_order_shards_hash16_to_hash32(TRUE);

   3. Dry-run again. An empty migration_plan result means t_order and
      t_order_item rows are placed according to the current 2 * 16 route.
      Check shard_distribution to confirm whether data is present across the
      expected 32 physical shards.

 Notes:
   - Stop order-writing services before executing the migration.
   - The procedure backs up all order shards to livestart_migration_backup.
   - The procedure is designed to be rerunnable by checking id existence.
 =========================================================================
*/

CREATE DATABASE IF NOT EXISTS `livestart_migration_backup`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

DELIMITER $$

DROP FUNCTION IF EXISTS `live_start`.`livestart_java_long_hash_positive`$$
CREATE FUNCTION `live_start`.`livestart_java_long_hash_positive`(p_user_id BIGINT)
RETURNS BIGINT
DETERMINISTIC
BEGIN
    RETURN (
        (
            (CAST(p_user_id AS UNSIGNED) & 4294967295)
            ^
            ((CAST(p_user_id AS UNSIGNED) >> 32) & 4294967295)
        ) & 2147483647
    );
END$$

DROP PROCEDURE IF EXISTS `live_start`.`sp_migrate_order_shards_hash16_to_hash32`$$
CREATE PROCEDURE `live_start`.`sp_migrate_order_shards_hash16_to_hash32`(IN p_execute BOOLEAN)
BEGIN
    DECLARE v_db_index INT DEFAULT 0;
    DECLARE v_table_index INT DEFAULT 0;
    DECLARE v_target_db_index INT DEFAULT 0;
    DECLARE v_target_table_index INT DEFAULT 0;
    DECLARE v_source_schema VARCHAR(64);
    DECLARE v_source_table VARCHAR(64);
    DECLARE v_target_schema VARCHAR(64);
    DECLARE v_target_table VARCHAR(64);
    DECLARE v_backup_table VARCHAR(128);
    DECLARE v_sql TEXT;

    DROP TABLE IF EXISTS `livestart_migration_backup`.`order_shard_migration_plan`;
    CREATE TABLE `livestart_migration_backup`.`order_shard_migration_plan` (
      `object_type` varchar(16) NOT NULL,
      `source_schema` varchar(64) NOT NULL,
      `source_table` varchar(64) NOT NULL,
      `target_schema` varchar(64) NOT NULL,
      `target_table` varchar(64) NOT NULL,
      `row_id` bigint NOT NULL,
      `business_key` varchar(128) NOT NULL,
      `user_id` bigint NOT NULL,
      PRIMARY KEY (`object_type`, `source_schema`, `source_table`, `row_id`),
      KEY `idx_target` (`target_schema`, `target_table`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    DROP TABLE IF EXISTS `livestart_migration_backup`.`order_shard_distribution`;
    CREATE TABLE `livestart_migration_backup`.`order_shard_distribution` (
      `object_type` varchar(16) NOT NULL,
      `source_schema` varchar(64) NOT NULL,
      `source_table` varchar(64) NOT NULL,
      `row_count` bigint NOT NULL,
      PRIMARY KEY (`object_type`, `source_schema`, `source_table`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    SET v_db_index = 0;
    WHILE v_db_index < 2 DO
        SET v_table_index = 0;
        WHILE v_table_index < 16 DO
            SET v_source_schema = CONCAT('ds_order_', v_db_index);
            SET v_source_table = CONCAT('t_order_', v_table_index);

            SET v_sql = CONCAT(
                'INSERT INTO `livestart_migration_backup`.`order_shard_distribution` ',
                '(`object_type`,`source_schema`,`source_table`,`row_count`) ',
                'SELECT ''order'', ''', v_source_schema, ''', ''', v_source_table, ''', COUNT(*) ',
                'FROM `', v_source_schema, '`.`', v_source_table, '`'
            );
            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            SET v_sql = CONCAT(
                'INSERT INTO `livestart_migration_backup`.`order_shard_migration_plan` ',
                '(`object_type`,`source_schema`,`source_table`,`target_schema`,`target_table`,`row_id`,`business_key`,`user_id`) ',
                'SELECT ''order'', ''', v_source_schema, ''', ''', v_source_table, ''', target_schema, target_table, id, order_no, user_id ',
                'FROM (',
                '  SELECT id, order_no, user_id, ',
                '         CONCAT(''ds_order_'', live_start.livestart_java_long_hash_positive(user_id) % 2) AS target_schema, ',
                '         CONCAT(''t_order_'', FLOOR(live_start.livestart_java_long_hash_positive(user_id) / 2) % 16) AS target_table ',
                '  FROM `', v_source_schema, '`.`', v_source_table, '`',
                ') x ',
                'WHERE NOT (target_schema = ''', v_source_schema, ''' AND target_table = ''', v_source_table, ''')'
            );
            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            SET v_source_table = CONCAT('t_order_item_', v_table_index);
            SET v_sql = CONCAT(
                'INSERT INTO `livestart_migration_backup`.`order_shard_distribution` ',
                '(`object_type`,`source_schema`,`source_table`,`row_count`) ',
                'SELECT ''order_item'', ''', v_source_schema, ''', ''', v_source_table, ''', COUNT(*) ',
                'FROM `', v_source_schema, '`.`', v_source_table, '`'
            );
            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            SET v_sql = CONCAT(
                'INSERT INTO `livestart_migration_backup`.`order_shard_migration_plan` ',
                '(`object_type`,`source_schema`,`source_table`,`target_schema`,`target_table`,`row_id`,`business_key`,`user_id`) ',
                'SELECT ''order_item'', ''', v_source_schema, ''', ''', v_source_table, ''', target_schema, target_table, id, check_code, user_id ',
                'FROM (',
                '  SELECT id, check_code, user_id, ',
                '         CONCAT(''ds_order_'', live_start.livestart_java_long_hash_positive(user_id) % 2) AS target_schema, ',
                '         CONCAT(''t_order_item_'', FLOOR(live_start.livestart_java_long_hash_positive(user_id) / 2) % 16) AS target_table ',
                '  FROM `', v_source_schema, '`.`', v_source_table, '`',
                ') x ',
                'WHERE NOT (target_schema = ''', v_source_schema, ''' AND target_table = ''', v_source_table, ''')'
            );
            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;

            SET v_table_index = v_table_index + 1;
        END WHILE;
        SET v_db_index = v_db_index + 1;
    END WHILE;

    IF p_execute THEN
        SET v_db_index = 0;
        WHILE v_db_index < 2 DO
            SET v_table_index = 0;
            WHILE v_table_index < 16 DO
                SET v_source_schema = CONCAT('ds_order_', v_db_index);

                SET v_source_table = CONCAT('t_order_', v_table_index);
                SET v_backup_table = CONCAT('t_order_ds', v_db_index, '_', v_table_index);
                SET v_sql = CONCAT('CREATE TABLE IF NOT EXISTS `livestart_migration_backup`.`', v_backup_table, '` LIKE `', v_source_schema, '`.`', v_source_table, '`');
                SET @sql = v_sql;
            PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SET v_sql = CONCAT(
                    'INSERT INTO `livestart_migration_backup`.`', v_backup_table, '` ',
                    'SELECT s.* FROM `', v_source_schema, '`.`', v_source_table, '` s ',
                    'WHERE NOT EXISTS (SELECT 1 FROM `livestart_migration_backup`.`', v_backup_table, '` b WHERE b.id = s.id)'
                );
                SET @sql = v_sql;
            PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;

                SET v_source_table = CONCAT('t_order_item_', v_table_index);
                SET v_backup_table = CONCAT('t_order_item_ds', v_db_index, '_', v_table_index);
                SET v_sql = CONCAT('CREATE TABLE IF NOT EXISTS `livestart_migration_backup`.`', v_backup_table, '` LIKE `', v_source_schema, '`.`', v_source_table, '`');
                SET @sql = v_sql;
            PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SET v_sql = CONCAT(
                    'INSERT INTO `livestart_migration_backup`.`', v_backup_table, '` ',
                    'SELECT s.* FROM `', v_source_schema, '`.`', v_source_table, '` s ',
                    'WHERE NOT EXISTS (SELECT 1 FROM `livestart_migration_backup`.`', v_backup_table, '` b WHERE b.id = s.id)'
                );
                SET @sql = v_sql;
            PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;

                SET v_table_index = v_table_index + 1;
            END WHILE;
            SET v_db_index = v_db_index + 1;
        END WHILE;

        SET v_db_index = 0;
        WHILE v_db_index < 2 DO
            SET v_table_index = 0;
            WHILE v_table_index < 16 DO
                SET v_source_schema = CONCAT('ds_order_', v_db_index);

                SET v_target_db_index = 0;
                WHILE v_target_db_index < 2 DO
                    SET v_target_table_index = 0;
                    WHILE v_target_table_index < 16 DO
                        SET v_target_schema = CONCAT('ds_order_', v_target_db_index);

                        SET v_source_table = CONCAT('t_order_', v_table_index);
                        SET v_target_table = CONCAT('t_order_', v_target_table_index);
                        IF NOT (v_source_schema = v_target_schema AND v_source_table = v_target_table) THEN
                            SET v_sql = CONCAT(
                                'INSERT INTO `', v_target_schema, '`.`', v_target_table, '` ',
                                'SELECT s.* FROM `', v_source_schema, '`.`', v_source_table, '` s ',
                                'WHERE live_start.livestart_java_long_hash_positive(s.user_id) % 2 = ', v_target_db_index, ' ',
                                'AND FLOOR(live_start.livestart_java_long_hash_positive(s.user_id) / 2) % 16 = ', v_target_table_index, ' ',
                                'AND NOT EXISTS (SELECT 1 FROM `', v_target_schema, '`.`', v_target_table, '` t WHERE t.id = s.id)'
                            );
                            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
                            EXECUTE stmt;
                            DEALLOCATE PREPARE stmt;
                        END IF;

                        SET v_source_table = CONCAT('t_order_item_', v_table_index);
                        SET v_target_table = CONCAT('t_order_item_', v_target_table_index);
                        IF NOT (v_source_schema = v_target_schema AND v_source_table = v_target_table) THEN
                            SET v_sql = CONCAT(
                                'INSERT INTO `', v_target_schema, '`.`', v_target_table, '` ',
                                'SELECT s.* FROM `', v_source_schema, '`.`', v_source_table, '` s ',
                                'WHERE live_start.livestart_java_long_hash_positive(s.user_id) % 2 = ', v_target_db_index, ' ',
                                'AND FLOOR(live_start.livestart_java_long_hash_positive(s.user_id) / 2) % 16 = ', v_target_table_index, ' ',
                                'AND NOT EXISTS (SELECT 1 FROM `', v_target_schema, '`.`', v_target_table, '` t WHERE t.id = s.id)'
                            );
                            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
                            EXECUTE stmt;
                            DEALLOCATE PREPARE stmt;
                        END IF;

                        SET v_target_table_index = v_target_table_index + 1;
                    END WHILE;
                    SET v_target_db_index = v_target_db_index + 1;
                END WHILE;

                SET v_table_index = v_table_index + 1;
            END WHILE;
            SET v_db_index = v_db_index + 1;
        END WHILE;

        SET v_db_index = 0;
        WHILE v_db_index < 2 DO
            SET v_table_index = 0;
            WHILE v_table_index < 16 DO
                SET v_source_schema = CONCAT('ds_order_', v_db_index);

                SET v_target_db_index = 0;
                WHILE v_target_db_index < 2 DO
                    SET v_target_table_index = 0;
                    WHILE v_target_table_index < 16 DO
                        SET v_target_schema = CONCAT('ds_order_', v_target_db_index);

                        SET v_source_table = CONCAT('t_order_item_', v_table_index);
                        SET v_target_table = CONCAT('t_order_item_', v_target_table_index);
                        IF NOT (v_source_schema = v_target_schema AND v_source_table = v_target_table) THEN
                            SET v_sql = CONCAT(
                                'DELETE s FROM `', v_source_schema, '`.`', v_source_table, '` s ',
                                'JOIN `', v_target_schema, '`.`', v_target_table, '` t ON t.id = s.id AND t.check_code = s.check_code ',
                                'WHERE live_start.livestart_java_long_hash_positive(s.user_id) % 2 = ', v_target_db_index, ' ',
                                'AND FLOOR(live_start.livestart_java_long_hash_positive(s.user_id) / 2) % 16 = ', v_target_table_index
                            );
                            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
                            EXECUTE stmt;
                            DEALLOCATE PREPARE stmt;
                        END IF;

                        SET v_source_table = CONCAT('t_order_', v_table_index);
                        SET v_target_table = CONCAT('t_order_', v_target_table_index);
                        IF NOT (v_source_schema = v_target_schema AND v_source_table = v_target_table) THEN
                            SET v_sql = CONCAT(
                                'DELETE s FROM `', v_source_schema, '`.`', v_source_table, '` s ',
                                'JOIN `', v_target_schema, '`.`', v_target_table, '` t ON t.id = s.id AND t.order_no = s.order_no ',
                                'WHERE live_start.livestart_java_long_hash_positive(s.user_id) % 2 = ', v_target_db_index, ' ',
                                'AND FLOOR(live_start.livestart_java_long_hash_positive(s.user_id) / 2) % 16 = ', v_target_table_index
                            );
                            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
                            EXECUTE stmt;
                            DEALLOCATE PREPARE stmt;
                        END IF;

                        SET v_target_table_index = v_target_table_index + 1;
                    END WHILE;
                    SET v_target_db_index = v_target_db_index + 1;
                END WHILE;

                SET v_table_index = v_table_index + 1;
            END WHILE;
            SET v_db_index = v_db_index + 1;
        END WHILE;
    END IF;

    SELECT
        object_type,
        source_schema,
        source_table,
        target_schema,
        target_table,
        COUNT(*) AS rows_to_move
    FROM `livestart_migration_backup`.`order_shard_migration_plan`
    GROUP BY object_type, source_schema, source_table, target_schema, target_table
    ORDER BY object_type, source_schema, source_table, target_schema, target_table;

    SELECT
        object_type,
        COUNT(*) AS configured_physical_shards,
        SUM(CASE WHEN row_count > 0 THEN 1 ELSE 0 END) AS non_empty_physical_shards,
        SUM(row_count) AS total_rows
    FROM `livestart_migration_backup`.`order_shard_distribution`
    GROUP BY object_type
    ORDER BY object_type;

    SELECT
        object_type,
        source_schema,
        source_table,
        row_count
    FROM `livestart_migration_backup`.`order_shard_distribution`
    ORDER BY object_type, source_schema, source_table;
END$$

DELIMITER ;




-- END 06_migrate_order_shards_hash16_to_hash32.sql

-- ============================================================================
-- BEGIN 11_migrate_user_ticket_shards.sql
-- ============================================================================
/*
 * 将历史 t_user_ticket 数据迁移到当前 2 库 * 16 表路由。
 * 先执行 CALL live_start.sp_migrate_user_ticket_shards(FALSE) 查看计划，
 * 确认后执行 CALL live_start.sp_migrate_user_ticket_shards(TRUE)。
 */

CREATE DATABASE IF NOT EXISTS `livestart_migration_backup`
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

DELIMITER $$

DROP FUNCTION IF EXISTS `live_start`.`livestart_java_long_hash_positive`$$
CREATE FUNCTION `live_start`.`livestart_java_long_hash_positive`(p_user_id BIGINT)
RETURNS BIGINT
DETERMINISTIC
BEGIN
    RETURN (((CAST(p_user_id AS UNSIGNED) & 4294967295)
        ^ ((CAST(p_user_id AS UNSIGNED) >> 32) & 4294967295)) & 2147483647);
END$$

DROP PROCEDURE IF EXISTS `live_start`.`sp_migrate_user_ticket_shards`$$
CREATE PROCEDURE `live_start`.`sp_migrate_user_ticket_shards`(IN p_execute BOOLEAN)
BEGIN
    DECLARE v_source_db INT DEFAULT 0;
    DECLARE v_source_table INT DEFAULT 0;
    DECLARE v_target_db INT DEFAULT 0;
    DECLARE v_target_table INT DEFAULT 0;
    DECLARE v_source_schema VARCHAR(64);
    DECLARE v_source_name VARCHAR(64);
    DECLARE v_target_schema VARCHAR(64);
    DECLARE v_target_name VARCHAR(64);
    DECLARE v_backup_name VARCHAR(128);
    DECLARE v_sql TEXT;

    DROP TABLE IF EXISTS `livestart_migration_backup`.`user_ticket_migration_plan`;
    CREATE TABLE `livestart_migration_backup`.`user_ticket_migration_plan` (
      `source_schema` varchar(64) NOT NULL,
      `source_table` varchar(64) NOT NULL,
      `target_schema` varchar(64) NOT NULL,
      `target_table` varchar(64) NOT NULL,
      `row_id` bigint NOT NULL,
      `check_code` varchar(64) NOT NULL,
      `user_id` bigint NOT NULL,
      PRIMARY KEY (`source_schema`, `source_table`, `row_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

    WHILE v_source_db < 2 DO
        SET v_source_table = 0;
        WHILE v_source_table < 16 DO
            SET v_source_schema = CONCAT('ds_order_', v_source_db);
            SET v_source_name = CONCAT('t_user_ticket_', v_source_table);
            SET v_sql = CONCAT(
                'INSERT INTO `livestart_migration_backup`.`user_ticket_migration_plan` ',
                'SELECT ''', v_source_schema, ''',''', v_source_name, ''',',
                'CONCAT(''ds_order_'', live_start.livestart_java_long_hash_positive(user_id) % 2),',
                'CONCAT(''t_user_ticket_'', FLOOR(live_start.livestart_java_long_hash_positive(user_id) / 2) % 16),',
                'id, check_code, user_id FROM `', v_source_schema, '`.`', v_source_name, '` ',
                'WHERE NOT (live_start.livestart_java_long_hash_positive(user_id) % 2 = ', v_source_db,
                ' AND FLOOR(live_start.livestart_java_long_hash_positive(user_id) / 2) % 16 = ', v_source_table, ')'
            );
            SET @sql = v_sql;
            PREPARE stmt FROM @sql;
            EXECUTE stmt;
            DEALLOCATE PREPARE stmt;
            SET v_source_table = v_source_table + 1;
        END WHILE;
        SET v_source_db = v_source_db + 1;
    END WHILE;

    IF p_execute THEN
        SET v_source_db = 0;
        WHILE v_source_db < 2 DO
            SET v_source_table = 0;
            WHILE v_source_table < 16 DO
                SET v_source_schema = CONCAT('ds_order_', v_source_db);
                SET v_source_name = CONCAT('t_user_ticket_', v_source_table);
                SET v_backup_name = CONCAT('t_user_ticket_ds', v_source_db, '_', v_source_table);
                SET v_sql = CONCAT('CREATE TABLE IF NOT EXISTS `livestart_migration_backup`.`',
                    v_backup_name, '` LIKE `', v_source_schema, '`.`', v_source_name, '`');
                SET @sql = v_sql;
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SET v_sql = CONCAT('INSERT IGNORE INTO `livestart_migration_backup`.`', v_backup_name,
                    '` SELECT s.* FROM `', v_source_schema, '`.`', v_source_name, '` s JOIN ',
                    '`livestart_migration_backup`.`user_ticket_migration_plan` p ON p.row_id=s.id ',
                    'AND p.source_schema=''', v_source_schema, ''' AND p.source_table=''', v_source_name, '''');
                SET @sql = v_sql;
                PREPARE stmt FROM @sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;

                SET v_target_db = 0;
                WHILE v_target_db < 2 DO
                    SET v_target_table = 0;
                    WHILE v_target_table < 16 DO
                        SET v_target_schema = CONCAT('ds_order_', v_target_db);
                        SET v_target_name = CONCAT('t_user_ticket_', v_target_table);
                        IF NOT (v_source_schema = v_target_schema AND v_source_name = v_target_name) THEN
                            SET v_sql = CONCAT('INSERT INTO `', v_target_schema, '`.`', v_target_name,
                                '` SELECT s.* FROM `', v_source_schema, '`.`', v_source_name, '` s ',
                                'WHERE live_start.livestart_java_long_hash_positive(s.user_id) % 2 = ', v_target_db,
                                ' AND FLOOR(live_start.livestart_java_long_hash_positive(s.user_id) / 2) % 16 = ', v_target_table,
                                ' AND NOT EXISTS (SELECT 1 FROM `', v_target_schema, '`.`', v_target_name, '` t WHERE t.id=s.id)');
                            SET @sql = v_sql;
                            PREPARE stmt FROM @sql;
                            EXECUTE stmt;
                            DEALLOCATE PREPARE stmt;

                            SET v_sql = CONCAT('DELETE s FROM `', v_source_schema, '`.`', v_source_name,
                                '` s JOIN `', v_target_schema, '`.`', v_target_name,
                                '` t ON t.id=s.id AND t.check_code=s.check_code WHERE ',
                                'live_start.livestart_java_long_hash_positive(s.user_id) % 2 = ', v_target_db,
                                ' AND FLOOR(live_start.livestart_java_long_hash_positive(s.user_id) / 2) % 16 = ', v_target_table);
                            SET @sql = v_sql;
                            PREPARE stmt FROM @sql;
                            EXECUTE stmt;
                            DEALLOCATE PREPARE stmt;
                        END IF;
                        SET v_target_table = v_target_table + 1;
                    END WHILE;
                    SET v_target_db = v_target_db + 1;
                END WHILE;
                SET v_source_table = v_source_table + 1;
            END WHILE;
            SET v_source_db = v_source_db + 1;
        END WHILE;
    END IF;

    SELECT * FROM `livestart_migration_backup`.`user_ticket_migration_plan`
    ORDER BY source_schema, source_table, row_id;
END$$

DELIMITER ;

-- END 11_migrate_user_ticket_shards.sql

-- ============================================================================
-- BEGIN 12_migrate_user_shards.sql
-- ============================================================================
/*
 * 用户库分片修复：统一为当前 ShardingSphere 规则
 *   hash     = positive Java Long.hashCode(user_id)
 *   database = hash % 2
 *   table    = floor(hash / 2) % 8
 *
 * 默认只预览：CALL live_start.sp_migrate_user_shards(FALSE);
 * 确认备份后执行：CALL live_start.sp_migrate_user_shards(TRUE);
 * 执行完成后运行 tools/verify_livestart_database.ps1。
 */

DELIMITER $$

DROP PROCEDURE IF EXISTS `live_start`.`sp_migrate_user_shards`$$
CREATE PROCEDURE `live_start`.`sp_migrate_user_shards`(IN p_execute BOOLEAN)
BEGIN
    DECLARE v_source_db INT DEFAULT 0;
    DECLARE v_source_table INT DEFAULT 0;
    DECLARE v_target_db INT DEFAULT 0;
    DECLARE v_target_table INT DEFAULT 0;
    DECLARE v_kind INT DEFAULT 0;
    DECLARE v_prefix VARCHAR(32);
    DECLARE v_sharding_column VARCHAR(32);
    DECLARE v_primary_key VARCHAR(32);
    DECLARE v_hash_expression VARCHAR(512);
    DECLARE v_sql LONGTEXT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    DROP TEMPORARY TABLE IF EXISTS tmp_user_shard_plan;
    CREATE TEMPORARY TABLE tmp_user_shard_plan (
        logical_table VARCHAR(32) NOT NULL,
        source_schema VARCHAR(32) NOT NULL,
        source_table VARCHAR(32) NOT NULL,
        target_schema VARCHAR(32) NOT NULL,
        target_table VARCHAR(32) NOT NULL,
        row_count BIGINT NOT NULL
    );

    IF p_execute THEN
        START TRANSACTION;
    END IF;

    SET v_kind = 0;
    WHILE v_kind < 3 DO
        SET v_prefix = CASE v_kind
            WHEN 0 THEN 't_user'
            WHEN 1 THEN 't_user_profile'
            ELSE 't_user_visitor'
        END;
        SET v_sharding_column = CASE v_kind WHEN 0 THEN 'id' ELSE 'user_id' END;
        SET v_primary_key = CASE v_kind WHEN 1 THEN 'user_id' ELSE 'id' END;
        SET v_hash_expression = CONCAT(
            '(((CAST(`', v_sharding_column, '` AS UNSIGNED) & 4294967295) ^ ',
            '((CAST(`', v_sharding_column, '` AS UNSIGNED) >> 32) & 4294967295)) & 2147483647)'
        );

        SET v_source_db = 0;
        WHILE v_source_db < 2 DO
            SET v_source_table = 0;
            WHILE v_source_table < 8 DO
                SET v_target_db = 0;
                WHILE v_target_db < 2 DO
                    SET v_target_table = 0;
                    WHILE v_target_table < 8 DO
                        IF v_source_db <> v_target_db OR v_source_table <> v_target_table THEN
                            SET @row_count = 0;
                            SET v_sql = CONCAT(
                                'SELECT COUNT(*) INTO @row_count FROM `ds_user_', v_source_db, '`.`',
                                v_prefix, '_', v_source_table, '` WHERE MOD(', v_hash_expression,
                                ',2)=', v_target_db, ' AND MOD(FLOOR(', v_hash_expression, '/2),8)=', v_target_table
                            );
                            SET @migration_sql = v_sql;
                            PREPARE stmt FROM @migration_sql;
                            EXECUTE stmt;
                            DEALLOCATE PREPARE stmt;

                            IF @row_count > 0 THEN
                                INSERT INTO tmp_user_shard_plan VALUES (
                                    v_prefix,
                                    CONCAT('ds_user_', v_source_db), CONCAT(v_prefix, '_', v_source_table),
                                    CONCAT('ds_user_', v_target_db), CONCAT(v_prefix, '_', v_target_table),
                                    @row_count
                                );

                                IF p_execute THEN
                                    SET @conflict_count = 0;
                                    SET v_sql = CONCAT(
                                        'SELECT COUNT(*) INTO @conflict_count FROM `ds_user_', v_source_db,
                                        '`.`', v_prefix, '_', v_source_table, '` source JOIN `ds_user_',
                                        v_target_db, '`.`', v_prefix, '_', v_target_table,
                                        '` target ON target.`', v_primary_key, '`=source.`', v_primary_key,
                                        '` WHERE MOD(', REPLACE(v_hash_expression, CONCAT('`', v_sharding_column, '`'),
                                                CONCAT('source.`', v_sharding_column, '`')), ',2)=', v_target_db,
                                        ' AND MOD(FLOOR(', REPLACE(v_hash_expression, CONCAT('`', v_sharding_column, '`'),
                                                CONCAT('source.`', v_sharding_column, '`')), '/2),8)=', v_target_table
                                    );
                                    SET @migration_sql = v_sql;
                                    PREPARE stmt FROM @migration_sql;
                                    EXECUTE stmt;
                                    DEALLOCATE PREPARE stmt;
                                    IF @conflict_count > 0 THEN
                                        SIGNAL SQLSTATE '45000'
                                            SET MESSAGE_TEXT = 'Target user shard already contains a source primary key';
                                    END IF;

                                    SET v_sql = CONCAT(
                                        'INSERT INTO `ds_user_', v_target_db, '`.`', v_prefix, '_', v_target_table,
                                        '` SELECT * FROM `ds_user_', v_source_db, '`.`', v_prefix, '_', v_source_table,
                                        '` WHERE MOD(', v_hash_expression, ',2)=', v_target_db,
                                        ' AND MOD(FLOOR(', v_hash_expression, '/2),8)=', v_target_table
                                    );
                                    SET @migration_sql = v_sql;
                                    PREPARE stmt FROM @migration_sql;
                                    EXECUTE stmt;
                                    DEALLOCATE PREPARE stmt;

                                    SET v_sql = CONCAT(
                                        'DELETE source FROM `ds_user_', v_source_db, '`.`', v_prefix, '_', v_source_table,
                                        '` source INNER JOIN `ds_user_', v_target_db, '`.`', v_prefix, '_', v_target_table,
                                        '` target ON target.`', v_primary_key, '`=source.`', v_primary_key,
                                        '` WHERE MOD(', REPLACE(v_hash_expression, CONCAT('`', v_sharding_column, '`'),
                                                CONCAT('source.`', v_sharding_column, '`')), ',2)=', v_target_db,
                                        ' AND MOD(FLOOR(', REPLACE(v_hash_expression, CONCAT('`', v_sharding_column, '`'),
                                                CONCAT('source.`', v_sharding_column, '`')), '/2),8)=', v_target_table
                                    );
                                    SET @migration_sql = v_sql;
                                    PREPARE stmt FROM @migration_sql;
                                    EXECUTE stmt;
                                    DEALLOCATE PREPARE stmt;
                                END IF;
                            END IF;
                        END IF;
                        SET v_target_table = v_target_table + 1;
                    END WHILE;
                    SET v_target_db = v_target_db + 1;
                END WHILE;
                SET v_source_table = v_source_table + 1;
            END WHILE;
            SET v_source_db = v_source_db + 1;
        END WHILE;
        SET v_kind = v_kind + 1;
    END WHILE;

    IF p_execute THEN
        SET v_source_db = 0;
        WHILE v_source_db < 2 DO
            SET v_source_table = 0;
            WHILE v_source_table < 8 DO
                SET v_sql = CONCAT(
                    'INSERT IGNORE INTO `live_start`.`t_user_phone_mapping` (`phone`,`user_id`) ',
                    'SELECT `phone`,`id` FROM `ds_user_', v_source_db, '`.`t_user_', v_source_table, '`'
                );
                SET @migration_sql = v_sql;
                PREPARE stmt FROM @migration_sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                SET @mapping_conflicts = 0;
                SET v_sql = CONCAT(
                    'SELECT COUNT(*) INTO @mapping_conflicts FROM `ds_user_', v_source_db,
                    '`.`t_user_', v_source_table, '` u LEFT JOIN `live_start`.`t_user_phone_mapping` m ',
                    'ON m.phone=u.phone AND m.user_id=u.id WHERE m.phone IS NULL'
                );
                SET @migration_sql = v_sql;
                PREPARE stmt FROM @migration_sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
                IF @mapping_conflicts > 0 THEN
                    SIGNAL SQLSTATE '45000'
                        SET MESSAGE_TEXT = 'User phone mapping conflicts with migrated users';
                END IF;
                SET v_source_table = v_source_table + 1;
            END WHILE;
            SET v_source_db = v_source_db + 1;
        END WHILE;
        COMMIT;
    END IF;

    SELECT * FROM tmp_user_shard_plan
    ORDER BY logical_table, source_schema, source_table, target_schema, target_table;
    SELECT logical_table, SUM(row_count) AS rows_to_move
    FROM tmp_user_shard_plan
    GROUP BY logical_table
    ORDER BY logical_table;
END$$

DELIMITER ;

-- END 12_migrate_user_shards.sql

-- ============================================================================
-- BEGIN 07_settlement_error_message_text_and_paid_only.sql (schema upgrade only)
-- ============================================================================
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
-- END settlement schema upgrade

-- ============================================================================
-- BEGIN 13_distribution_event_source_link.sql
-- ============================================================================
-- Link the merchant event shown in search to its distribution sale stages.
-- Run after 04_feature_and_job_upgrade.sql on existing databases.
USE `live_start`;

DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_distribution_event_source_link$$
CREATE PROCEDURE upgrade_distribution_event_source_link()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND COLUMN_NAME = 'source_event_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD COLUMN `source_event_id` bigint NULL COMMENT 'Merchant event ID for distribution copy' AFTER `id`;
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND INDEX_NAME IN ('idx_source_event_id', 'uq_source_event_id')
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD INDEX `idx_source_event_id` (`source_event_id`);
  END IF;

  -- Only backfill a published copy when exactly one merchant event matches.
  DROP TEMPORARY TABLE IF EXISTS distribution_event_source_matches;
  CREATE TEMPORARY TABLE distribution_event_source_matches AS
    SELECT d.id AS distribution_id, MIN(m.id) AS merchant_id
    FROM `live_start`.`t_event` AS d
    JOIN `live_start`.`t_event_sale_stage` AS stage ON stage.event_id = d.id
    JOIN `live_start`.`t_event` AS m
      ON m.id <> d.id AND m.title = d.title
      AND m.venue_id = d.venue_id AND m.start_time = d.start_time
    LEFT JOIN `live_start`.`t_event_sale_stage` AS merchant_stage ON merchant_stage.event_id = m.id
    WHERE d.source_event_id IS NULL AND merchant_stage.id IS NULL
    GROUP BY d.id
    HAVING COUNT(DISTINCT m.id) = 1;

  UPDATE `live_start`.`t_event` AS published
  JOIN distribution_event_source_matches AS matches ON matches.distribution_id = published.id
  SET published.source_event_id = matches.merchant_id;
  DROP TEMPORARY TABLE distribution_event_source_matches;

  IF NOT EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND INDEX_NAME = 'uq_source_event_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event`
      ADD UNIQUE INDEX `uq_source_event_id` (`source_event_id`);
  END IF;

  IF EXISTS (
    SELECT 1 FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = 'live_start' AND TABLE_NAME = 't_event' AND INDEX_NAME = 'idx_source_event_id'
  ) THEN
    ALTER TABLE `live_start`.`t_event` DROP INDEX `idx_source_event_id`;
  END IF;
END$$
CALL upgrade_distribution_event_source_link()$$
DROP PROCEDURE upgrade_distribution_event_source_link$$
DELIMITER ;

-- END 13_distribution_event_source_link.sql

-- ============================================================================
-- BEGIN 14_verify_ticket_lookup_indexes.sql
-- ============================================================================
-- Existing order shards created with CREATE TABLE AS SELECT have no indexes.
-- Run once after 02_livestart_sharded_schema.sql; safe to rerun.
USE `live_start`;
DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_ticket_lookup_indexes$$
CREATE PROCEDURE upgrade_ticket_lookup_indexes()
BEGIN
  DECLARE database_index INT DEFAULT 0;
  DECLARE table_index INT;
  DECLARE schema_name VARCHAR(32);
  DECLARE order_table VARCHAR(32);
  DECLARE item_table VARCHAR(32);

  WHILE database_index < 2 DO
    SET schema_name = CONCAT('ds_order_', database_index);
    SET table_index = 0;
    WHILE table_index < 16 DO
      SET order_table = CONCAT('t_order_', table_index);
      SET item_table = CONCAT('t_order_item_', table_index);

      IF EXISTS (SELECT 1 FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = order_table)
         AND NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                         WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = order_table
                           AND COLUMN_NAME = 'order_no' AND SEQ_IN_INDEX = 1) THEN
        SET @verify_index_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', order_table,
                                       '` ADD INDEX `idx_verify_order_no` (`order_no`)');
        PREPARE verify_index_stmt FROM @verify_index_sql;
        EXECUTE verify_index_stmt;
        DEALLOCATE PREPARE verify_index_stmt;
      END IF;

      IF EXISTS (SELECT 1 FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = item_table)
         AND NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                         WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = item_table
                           AND COLUMN_NAME = 'check_code' AND SEQ_IN_INDEX = 1) THEN
        SET @verify_index_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', item_table,
                                       '` ADD INDEX `idx_verify_check_code` (`check_code`)');
        PREPARE verify_index_stmt FROM @verify_index_sql;
        EXECUTE verify_index_stmt;
        DEALLOCATE PREPARE verify_index_stmt;
      END IF;

      SET table_index = table_index + 1;
    END WHILE;
    SET database_index = database_index + 1;
  END WHILE;
END$$
CALL upgrade_ticket_lookup_indexes()$$
DROP PROCEDURE upgrade_ticket_lookup_indexes$$
DELIMITER ;

-- END 14_verify_ticket_lookup_indexes.sql

-- ============================================================================
-- BEGIN 15_verify_ticket_records.sql
-- ============================================================================
-- Upgrade existing order item shards. Safe to rerun after 02_livestart_sharded_schema.sql.
USE `live_start`;
DELIMITER $$
DROP PROCEDURE IF EXISTS upgrade_ticket_verify_records$$
CREATE PROCEDURE upgrade_ticket_verify_records()
BEGIN
  DECLARE database_index INT DEFAULT 0;
  DECLARE table_index INT;
  DECLARE schema_name VARCHAR(32);
  DECLARE target_table VARCHAR(32);

  WHILE database_index < 2 DO
    SET schema_name = CONCAT('ds_order_', database_index);
    SET table_index = 0;
    WHILE table_index < 16 DO
      SET target_table = CONCAT('t_order_item_', table_index);
      IF EXISTS (SELECT 1 FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = target_table) THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = target_table
                         AND COLUMN_NAME = 'checked_at') THEN
          SET @verify_records_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', target_table,
                                           '` ADD COLUMN `checked_at` datetime DEFAULT NULL COMMENT ''成功核销时间''');
          PREPARE verify_records_stmt FROM @verify_records_sql;
          EXECUTE verify_records_stmt;
          DEALLOCATE PREPARE verify_records_stmt;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = target_table
                         AND COLUMN_NAME = 'checked_by') THEN
          SET @verify_records_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', target_table,
                                           '` ADD COLUMN `checked_by` bigint DEFAULT NULL COMMENT ''核销操作人用户ID''');
          PREPARE verify_records_stmt FROM @verify_records_sql;
          EXECUTE verify_records_stmt;
          DEALLOCATE PREPARE verify_records_stmt;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                       WHERE TABLE_SCHEMA = schema_name AND TABLE_NAME = target_table
                         AND COLUMN_NAME = 'checked_at' AND SEQ_IN_INDEX = 1) THEN
          SET @verify_records_sql = CONCAT('ALTER TABLE `', schema_name, '`.`', target_table,
                                           '` ADD INDEX `idx_checked_at` (`checked_at`)');
          PREPARE verify_records_stmt FROM @verify_records_sql;
          EXECUTE verify_records_stmt;
          DEALLOCATE PREPARE verify_records_stmt;
        END IF;
      END IF;
      SET table_index = table_index + 1;
    END WHILE;
    SET database_index = database_index + 1;
  END WHILE;
END$$
CALL upgrade_ticket_verify_records()$$
DROP PROCEDURE upgrade_ticket_verify_records$$
DELIMITER ;

-- END 15_verify_ticket_records.sql

-- ============================================================================
-- BEGIN 16_settlement_notification_read.sql
-- ============================================================================
-- Existing databases: create the settlement notification state table before starting settlement.
USE `live_start`;

CREATE TABLE IF NOT EXISTS `t_settlement_notification_read` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '后台用户ID',
  `notification_key` varchar(120) NOT NULL COMMENT '通知唯一键',
  `read_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '已读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_user_notification` (`user_id`, `notification_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算通知已读记录';

-- END 16_settlement_notification_read.sql

-- ============================================================================
-- BEGIN 17_artist_wallet_withdrawal.sql
-- ============================================================================
/*
 * 艺人推广收益钱包与提现表。
 * 执行顺序：09_distribution_tables_upgrade.sql 之后。
 * 本脚本只新增表，不删除历史数据。
 */
USE `live_start`;

CREATE TABLE IF NOT EXISTS `t_artist_wallet_account` (
  `artist_id` bigint NOT NULL COMMENT '艺人用户ID，关联 t_user.id',
  `available_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '可提现余额',
  `frozen_amount` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '提现冻结金额',
  `total_earned` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计入账金额',
  `total_withdrawn` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '累计提现成功金额',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`artist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人推广收益钱包';

CREATE TABLE IF NOT EXISTS `t_artist_wallet_ledger` (
  `id` bigint NOT NULL COMMENT '账本流水ID',
  `artist_id` bigint NOT NULL COMMENT '艺人用户ID',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型：COMMISSION、WITHDRAW_FREEZE、WITHDRAW_RELEASE、WITHDRAW_PAID、REFUND',
  `biz_no` varchar(64) NOT NULL COMMENT '业务唯一号',
  `direction` tinyint NOT NULL COMMENT '方向 1:增加 2:减少',
  `amount` decimal(12,2) NOT NULL COMMENT '变动金额，正数',
  `balance_after` decimal(12,2) NOT NULL COMMENT '变动后可用余额',
  `remark` varchar(255) DEFAULT NULL COMMENT '流水说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artist_wallet_biz` (`artist_id`,`biz_type`,`biz_no`),
  KEY `idx_artist_wallet_time` (`artist_id`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人钱包不可变账本';

CREATE TABLE IF NOT EXISTS `t_artist_withdrawal` (
  `id` bigint NOT NULL COMMENT '提现申请ID',
  `request_no` varchar(64) NOT NULL COMMENT '提现请求幂等号',
  `artist_id` bigint NOT NULL COMMENT '艺人用户ID',
  `amount` decimal(12,2) NOT NULL COMMENT '提现金额',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '状态 0:待审核 1:处理中 2:已完成 3:已拒绝 4:已取消',
  `account_type` varchar(32) NOT NULL COMMENT '收款账户类型',
  `account_no` varchar(128) NOT NULL COMMENT '收款账号快照',
  `account_name` varchar(128) DEFAULT NULL COMMENT '收款人姓名快照',
  `external_no` varchar(128) DEFAULT NULL COMMENT '外部支付流水号',
  `audit_remark` varchar(255) DEFAULT NULL COMMENT '审核说明',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `completed_time` datetime DEFAULT NULL COMMENT '完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_artist_withdrawal_request` (`request_no`),
  KEY `idx_artist_withdrawal_status` (`artist_id`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='艺人提现申请';

-- END 17_artist_wallet_withdrawal.sql
