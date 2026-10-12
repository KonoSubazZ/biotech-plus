-- ============================================================================
-- 样本编号字段统一命名：sample_file.barcode → subbarcode
-- 为什么：
--   同一个「样本编号（源表列 BARCODE）」在本库里原来有两种叫法 —— 样本侧叫 barcode，
--   实验室/报告侧（qc_record、analysis_data、analysis_report、report_variant_history）叫 subbarcode。
--   本次统一为 subbarcode，JOIN / 接口 / Excel 导入导出只有一种叫法。
-- 影响：sample_file 列名 + 唯一键名（uk_sample_file_barcode → uk_sample_file_subbarcode）。
--       不含 analysis_data.barcode（那是患者编号，语义不同，保持原样）。
-- 幂等：先查 information_schema，列/索引还在才改；重复执行无副作用。
-- 依据：ai-rules/04-db-schema.md §6（迁移脚本只增不改、可重复执行）
-- ============================================================================

SET NAMES utf8mb4;

-- 1. 列改名（仅当旧列在、新列不在时执行）
SET @has_old_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sample_file' AND column_name = 'barcode');
SET @has_new_column = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'sample_file' AND column_name = 'subbarcode');

SET @ddl = IF(@has_old_column = 1 AND @has_new_column = 0,
    'ALTER TABLE sample_file CHANGE COLUMN barcode subbarcode varchar(100) COMMENT ''样本编号（源表列 BARCODE）''',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. 唯一键改名（注意：information_schema.statistics 每个索引列一行，命中数 > 0 即存在）
SET @has_old_key = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'sample_file' AND index_name = 'uk_sample_file_barcode');

SET @ddl = IF(@has_old_key > 0,
    'ALTER TABLE sample_file RENAME INDEX uk_sample_file_barcode TO uk_sample_file_subbarcode',
    'DO 0');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. 结果自检
SELECT column_name, column_type, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'sample_file' AND column_name IN ('barcode', 'subbarcode');

SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns_in_index
FROM information_schema.statistics
WHERE table_schema = DATABASE() AND table_name = 'sample_file' AND index_name LIKE 'uk_sample_file%'
GROUP BY index_name;
