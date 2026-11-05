-- =====================================================================================
-- 数据库备份：全量改成「一组库一个包」（绑定备份）
--
-- 为什么（用户 2026-10-29）：ry-vue 与 nkb 是一套系统的两个库，必须同生共死 ——
--   要备份都备份、要恢复都恢复。原来「每库一份全量」会出现「只备了一个库」的半残状态，
--   恢复时还得人工判断两份文件的时点是否一致。
--   改成：一次 mysqldump --databases <库1> <库2> 出一个文件、一个基线位点、一套 md5、一行记录。
--   binlog 增量本来就是一组库一个包（backup_db_binlog 不用改）。
--
-- 幂等：列改名先查 information_schema；合并存量行用临时表；索引先删后建。
-- 存量数据（旧口径每库一行）按 (instance, backup_date, backup_type) 合并成一行：
--   db_scope 逗号拼接、table_count/db_table_total/local_size 求和；local_path/local_md5 保留最早那行的
--   （历史行指向的是旧口径的单库文件，说明见本文件末尾）。
-- =====================================================================================

USE `ry-vue`;
SET NAMES utf8mb4;

-- -------------------------------------------------------------------------------------
-- 1. db_name → db_scope（本包覆盖的库，逗号分隔）
-- -------------------------------------------------------------------------------------

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'backup_db_full' AND COLUMN_NAME = 'db_name');
SET @s := IF(@c = 1,
  'ALTER TABLE `backup_db_full` CHANGE COLUMN `db_name` `db_scope` varchar(200) NOT NULL COMMENT ''本包覆盖的库（逗号分隔；绑定备份=一组库一个包）''',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -------------------------------------------------------------------------------------
-- 2. 合并旧口径的「每库一行」为「一组一行」（必须在换唯一键之前做，否则撞旧唯一键）
-- -------------------------------------------------------------------------------------

DROP TEMPORARY TABLE IF EXISTS tmp_bdf_merge;
CREATE TEMPORARY TABLE tmp_bdf_merge AS
SELECT `instance`, `backup_date`, `backup_type`,
       MIN(`id`)                        AS keep_id,
       GROUP_CONCAT(`db_scope` ORDER BY `db_scope`) AS scope,
       SUM(`table_count`)               AS tc,
       SUM(`db_table_total`)            AS tt,
       SUM(`local_size`)                AS ls
FROM `backup_db_full`
GROUP BY `instance`, `backup_date`, `backup_type`
HAVING COUNT(*) > 1;

UPDATE `backup_db_full` f
  JOIN tmp_bdf_merge m ON f.`id` = m.keep_id
SET f.`db_scope`       = m.scope,
    f.`table_count`    = m.tc,
    f.`db_table_total` = m.tt,
    f.`local_size`     = m.ls,
    f.`update_time`    = NOW();

DELETE f FROM `backup_db_full` f
  JOIN tmp_bdf_merge m
    ON f.`instance` = m.`instance` AND f.`backup_date` = m.`backup_date`
   AND f.`backup_type` = m.`backup_type` AND f.`id` <> m.keep_id;

DROP TEMPORARY TABLE IF EXISTS tmp_bdf_merge;

-- -------------------------------------------------------------------------------------
-- 3. 唯一键：按库名 → 按实例 + 日期 + 类型（一个包一天一条）
-- -------------------------------------------------------------------------------------

SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'backup_db_full' AND INDEX_NAME = 'uk_bdf_db_date_type');
SET @s := IF(@c > 0, 'ALTER TABLE `backup_db_full` DROP INDEX `uk_bdf_db_date_type`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'backup_db_full' AND INDEX_NAME = 'uk_bdf_inst_date_type');
SET @s := IF(@c = 0,
  'ALTER TABLE `backup_db_full` ADD UNIQUE KEY `uk_bdf_inst_date_type` (`instance`, `backup_date`, `backup_type`)',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 库名过滤（页面按「包含某库」筛）
SET @c := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'backup_db_full' AND INDEX_NAME = 'idx_bdf_scope');
SET @s := IF(@c = 0, 'ALTER TABLE `backup_db_full` ADD KEY `idx_bdf_scope` (`db_scope`)', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

ALTER TABLE `backup_db_full` COMMENT = '数据库全量备份记录（一组库一个包：绑定备份）';

-- -------------------------------------------------------------------------------------
-- 4. 自检
-- -------------------------------------------------------------------------------------

SELECT 'columns' AS item, GROUP_CONCAT(CONCAT(COLUMN_NAME, ':', COLUMN_TYPE) ORDER BY ORDINAL_POSITION) AS detail
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'backup_db_full' AND COLUMN_NAME IN ('db_scope', 'table_count', 'db_table_total')
UNION ALL
SELECT 'indexes', GROUP_CONCAT(DISTINCT INDEX_NAME)
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'backup_db_full'
UNION ALL
SELECT 'rows', CAST(COUNT(*) AS CHAR) FROM `backup_db_full`
UNION ALL
SELECT 'row_detail', GROUP_CONCAT(CONCAT(id, '|', db_scope, '|', table_count, '/', db_table_total) ORDER BY id)
FROM `backup_db_full`;
