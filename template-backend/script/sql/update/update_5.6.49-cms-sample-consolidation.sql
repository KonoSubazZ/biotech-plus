-- 5.6.49：样本表收敛为唯一 cms_sample
-- 背景：旧 cms_sample（122 列录单样本表）整体下线；sample_biotech（药企 CMS 接口字段）改名为 cms_sample。
-- 业务键从 (cms_order_id, cms_sample_id) 改为 (tenant_id, subbarcode)——CMS 内部主键调用方拿不到。
-- 执行前请先备份：mysqldump ry-vue cms_sample sample_biotech backup_sample_file qc_failed_notify
-- 说明：旧表 35 行与 backup_sample_file 均为测试数据，本脚本不迁移数据（迁移需按业务口径单独做）。

SET NAMES utf8mb4;

-- 1. 旧 cms_sample 改名隔离（存在才改）
SET @has_old := (SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = 'cms_sample');
SET @sql := IF(@has_old > 0,
    'RENAME TABLE `cms_sample` TO `cms_sample_legacy_20261031`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 新表去掉 CMS 内部主键
SET @has_order_col := (SELECT COUNT(*) FROM information_schema.columns
                       WHERE table_schema = DATABASE() AND table_name = 'sample_biotech'
                         AND column_name = 'biotech_cms_order_id');
SET @sql := IF(@has_order_col > 0,
    'ALTER TABLE `sample_biotech` DROP INDEX `uk_sample_biotech_cms_identity`, DROP COLUMN `biotech_cms_order_id`, DROP COLUMN `biotech_cms_sample_id`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 业务键改为 (tenant_id, biotech_subbarcode)
SET @has_uk := (SELECT COUNT(*) FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name = 'sample_biotech'
                  AND index_name = 'uk_cms_sample_subbarcode');
SET @sql := IF(@has_uk = 0,
    'ALTER TABLE `sample_biotech` ADD UNIQUE KEY `uk_cms_sample_subbarcode` (`tenant_id`, `biotech_subbarcode`)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 新表占用 cms_sample 名字
SET @has_new := (SELECT COUNT(*) FROM information_schema.tables
                 WHERE table_schema = DATABASE() AND table_name = 'sample_biotech');
SET @sql := IF(@has_new > 0,
    'RENAME TABLE `sample_biotech` TO `cms_sample`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5. 旧样本备份明细：测试数据不保留，只留结构
SET @has_backup := (SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'backup_sample_file');
SET @sql := IF(@has_backup > 0, 'TRUNCATE TABLE `backup_sample_file`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. 通知单里指向旧 cms_sample.id 的引用失效
SET @has_notify := (SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'qc_failed_notify'
                      AND column_name = 'sample_file_id');
SET @sql := IF(@has_notify > 0, 'UPDATE `qc_failed_notify` SET `sample_file_id` = NULL', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. 删旧表
SET @has_legacy := (SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = DATABASE() AND table_name = 'cms_sample_legacy_20261031');
SET @sql := IF(@has_legacy > 0, 'DROP TABLE `cms_sample_legacy_20261031`', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'cms_sample columns' AS item, COUNT(*) AS value FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'cms_sample';
