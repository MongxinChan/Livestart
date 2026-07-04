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
            CAST(p_user_id DIV 4294967296 AS UNSIGNED)
            ^
            CAST((p_user_id & 4294967295) AS UNSIGNED)
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



