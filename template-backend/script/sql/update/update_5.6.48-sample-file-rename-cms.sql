-- ----------------------------------------------------------------------------
-- 5.6.48：旧样本表改名 + 字段命名对齐新表 sample_biotech
--
-- 1) sample_file -> cms_sample：旧表是「接口字段未定稿」版本，改名归档只读；
--    新表 sample_biotech（iPaaS 契约 153 列）承接药企订单样本。
-- 2) project_config 的两个编号列改名为「新表去前缀后的语义名」：
--    contract_code -> contract_no、study_plan_no -> scheme_no。
--    全库同一语义只留一种写法：合同编号 = *_no、研究方案编号 = scheme_no。
-- 3) RENAME TABLE 不改索引名，这里顺带把 uk_sample_file_subbarcode 改成 uk_cms_*。
--
-- 可重复执行：每一步先查 information_schema 再决定是否执行。
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

-- 1) sample_file -> cms_sample
SET @has_table := (SELECT COUNT(*) FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'sample_file');
SET @sql := IF(@has_table = 1,
  'RENAME TABLE `sample_file` TO `cms_sample`',
  'SELECT ''sample_file 已改名，跳过'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 1.1) 索引名同步
SET @has_old_idx := (SELECT COUNT(*) FROM information_schema.statistics
                     WHERE table_schema = DATABASE() AND table_name = 'cms_sample'
                       AND index_name = 'uk_sample_file_subbarcode');
SET @sql := IF(@has_old_idx = 1,
  'ALTER TABLE `cms_sample` RENAME INDEX `uk_sample_file_subbarcode` TO `uk_cms_sample_subbarcode`',
  'SELECT ''索引已改名，跳过'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) project_config.contract_code -> contract_no
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'project_config'
                   AND column_name = 'contract_code');
SET @sql := IF(@has_col = 1,
  'ALTER TABLE `project_config` CHANGE COLUMN `contract_code` `contract_no` varchar(100) DEFAULT NULL COMMENT ''合同编号（仅项目行有值；展示字段）''',
  'SELECT ''contract_code 已改名，跳过'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3) project_config.study_plan_no -> scheme_no
SET @has_col := (SELECT COUNT(*) FROM information_schema.columns
                 WHERE table_schema = DATABASE() AND table_name = 'project_config'
                   AND column_name = 'study_plan_no');
SET @sql := IF(@has_col = 1,
  'ALTER TABLE `project_config` CHANGE COLUMN `study_plan_no` `scheme_no` varchar(100) DEFAULT NULL COMMENT ''研究方案编号（仅项目行有值；展示字段）''',
  'SELECT ''study_plan_no 已改名，跳过'' AS note');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
