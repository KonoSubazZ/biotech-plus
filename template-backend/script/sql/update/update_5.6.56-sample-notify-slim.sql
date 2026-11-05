-- ============================================================================
-- sample_notify 精简：只留样本基础信息 + 流程/文件字段
--
-- 1) 字段名与 cms_sample 对齐（同名同义，避免一张表一个叫法）：
--      study_no        → scheme_no       （研究方案编号，= cms_sample.scheme_no）
--      participant_no  → subject_no      （试验参与者编号，= cms_sample.subject_no）
--      product         → product_name    （产品名称，= cms_sample.product_name）
-- 2) file_path 改为 file_name：只存文件名，绝对路径继续用四个 *_path 冗余
-- 3) 删除通知内容字段与内部关联主键：
--      sample_file_id, wet_lab_qc_id, bioinfo_qc_id, analysis_time, qc_stage,
--      terminate_measure, remedy, remedy_other, reporter, report_time, reviewer, review_time
--    其中 reason / delay_reason / expected_report_date 保留（邮件正文实时要用）
--
-- 幂等：每步先查 information_schema，可反复跑。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 biotech < update_5.6.56-sample-notify-slim.sql
-- ============================================================================

SET NAMES utf8mb4;

-- 1) study_no → scheme_no
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify' AND COLUMN_NAME = 'study_no');
SET @sql = IF(@has = 1,
    'ALTER TABLE `sample_notify` CHANGE COLUMN `study_no` `scheme_no` varchar(128) DEFAULT NULL COMMENT ''研究方案编号（= cms_sample.scheme_no）''',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 2) participant_no → subject_no
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify' AND COLUMN_NAME = 'participant_no');
SET @sql = IF(@has = 1,
    'ALTER TABLE `sample_notify` CHANGE COLUMN `participant_no` `subject_no` varchar(128) DEFAULT NULL COMMENT ''试验参与者编号（= cms_sample.subject_no）''',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 3) product → product_name
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify' AND COLUMN_NAME = 'product');
SET @sql = IF(@has = 1,
    'ALTER TABLE `sample_notify` CHANGE COLUMN `product` `product_name` varchar(256) DEFAULT NULL COMMENT ''产品名称（= cms_sample.product_name）''',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 4) 新增 file_name 并从 file_path 回填
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify' AND COLUMN_NAME = 'file_name');
SET @sql = IF(@has = 0,
    'ALTER TABLE `sample_notify` ADD COLUMN `file_name` varchar(255) DEFAULT NULL COMMENT ''交付文件名（不含路径；路径见四个 *_path）'' AFTER `signed_pdf_path`',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify' AND COLUMN_NAME = 'file_path');
SET @sql = IF(@has = 1,
    'UPDATE `sample_notify` SET `file_name` = SUBSTRING_INDEX(`file_path`, ''/'', -1) WHERE `file_path` IS NOT NULL AND `file_path` <> ''''',
    'SELECT 1');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- 5) 删除不再使用的列（逐列判断，重复跑不报错）
SET @cols = 'sample_file_id,wet_lab_qc_id,bioinfo_qc_id,analysis_time,qc_stage,terminate_measure,remedy,remedy_other,reporter,report_time,reviewer,review_time,file_path';
SET @sql = (SELECT IF(COUNT(*) > 0,
    CONCAT('ALTER TABLE `sample_notify` ', GROUP_CONCAT(CONCAT('DROP COLUMN `', COLUMN_NAME, '`') ORDER BY COLUMN_NAME SEPARATOR ', ')),
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
      AND FIND_IN_SET(COLUMN_NAME, @cols) > 0);
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;
