-- 5.6.60 · 报告位点人工修改统一为 report_variant_edits，删除两列旧的覆盖表
--
-- 背景（2026-11-05 用户口径）：
--   位点人工修改（父靶点 / 胚系临床意义 / 突变说明）统一存一个报告级 JSON，
--   生成报告时解析进报告 JSON；审核通过时从报告 JSON 冻结 history_*。
--   历史命中（来源=历史）的位点只读，不允许写 edits。
--
-- 形态：
--   {"BRCA1_A2825E_CR_ALL:24":{"parentMutationIds":[1,2],"clinicalSignificance":1,"mutationDescription":"..."}}
--   键 = gene_variant_sourceType:sourceId（保证同报告内唯一；不同源表 id 会撞，故带 sourceType）
--   值 = 三个可选字段；空对象视为删除该键；整列为 NULL = 无人工修改
--
-- 同时删除已被它取代的两列：
--   report_variant_notes          （旧：只存突变说明）
--   report_germline_significance  （旧：只存临床意义）
--
-- 幂等：逐列查 information_schema，存在才执行；可重复执行。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 ry-vue < update_5.6.60-report-variant-edits.sql
SET NAMES utf8mb4;

SET @add_report_variant_edits := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `analysis_report` ADD COLUMN `report_variant_edits` json NULL DEFAULT NULL COMMENT ''本次报告的人工修改：{"BRCA1_A2825E_CR_ALL:24":{"parentMutationIds":[1,2],"clinicalSignificance":1,"mutationDescription":"..."}}''',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_report'
      AND COLUMN_NAME = 'report_variant_edits'
);
PREPARE add_report_variant_edits_stmt FROM @add_report_variant_edits;
EXECUTE add_report_variant_edits_stmt;
DEALLOCATE PREPARE add_report_variant_edits_stmt;

SET @drop_report_variant_notes := (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE `analysis_report` DROP COLUMN `report_variant_notes`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_report'
      AND COLUMN_NAME = 'report_variant_notes'
);
PREPARE drop_report_variant_notes_stmt FROM @drop_report_variant_notes;
EXECUTE drop_report_variant_notes_stmt;
DEALLOCATE PREPARE drop_report_variant_notes_stmt;

SET @drop_report_germline_significance := (
    SELECT IF(COUNT(*) > 0,
              'ALTER TABLE `analysis_report` DROP COLUMN `report_germline_significance`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_report'
      AND COLUMN_NAME = 'report_germline_significance'
);
PREPARE drop_report_germline_significance_stmt FROM @drop_report_germline_significance;
EXECUTE drop_report_germline_significance_stmt;
DEALLOCATE PREPARE drop_report_germline_significance_stmt;
