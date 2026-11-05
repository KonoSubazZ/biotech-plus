-- 5.6.58 · analysis_report 增加「预览阶段人工修改的位点说明」列
--
-- 为什么：报告预览 › 肿瘤遗传风险（胚系）的「突变说明」默认由 HGVS 译文 + NKB 位点说明实时拼出，
--         解读人员需要在未生成报告前人工改写，且改后的文本要进生成报告的 JSON（germlineVariants.items[].mutationDescription）。
--         预览 JSON 本身不落库（见 report-module-design §0.4），所以人工改动只能落到「输入侧」。
-- 口径（2026-11-04 用户确认）：
--   1) 报告粒度：同一 analysis 下多份报告互不覆盖（与改靶/临床意义不同，那两个是批次级共享）
--   2) 只存「人工改过」的位点：{"CR_ALL:44":"文本"}，键 = 来源类型:位点ID；没改的位点不写，整列可为 NULL
--   3) 空文本保存 = 恢复默认（删键）
--   4) 只有可生成报告的状态（31/32/33/52）允许写；40+ 已送审/已发送只读
-- 幂等：先查 information_schema，列已存在则跳过；可重复执行。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 biotech < update_5.6.58-report-variant-notes.sql
SET NAMES utf8mb4;

SET @add_report_variant_notes := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `analysis_report` ADD COLUMN `report_variant_notes` json NULL DEFAULT NULL COMMENT ''预览阶段人工修改的位点说明：{"CR_ALL:44":"文本"}，键=来源类型:位点ID；NULL=全部用默认'' AFTER `report_json_path`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_report'
      AND COLUMN_NAME = 'report_variant_notes'
);

PREPARE add_report_variant_notes_stmt FROM @add_report_variant_notes;
EXECUTE add_report_variant_notes_stmt;
DEALLOCATE PREPARE add_report_variant_notes_stmt;
