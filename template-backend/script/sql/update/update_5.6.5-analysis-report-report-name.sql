-- 5.6.5 · analysis_report 增加「报告名」列
--
-- 为什么：报告名由 report_template.report_name 渲染得出，生成时存档一份 snapshot：
--         报告列表/交付下载要按它显示，且模板改了命名规则也不该影响已生成的报告。
-- 口径：存渲染结果（静态文本 + 变量值原样），不含 DOCX/JSON 文件名上的时间与随机后缀；
--       未配置命名模板时为空，表示用默认文件名口径（report-<reportId>-<templateCode>-...）。
-- 幂等：先查 information_schema，列已存在则跳过；可重复执行。
SET NAMES utf8mb4;

SET @add_report_name := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `analysis_report` ADD COLUMN `report_name` VARCHAR(500) NULL DEFAULT NULL COMMENT ''最近一次生成的报告名（命名模板渲染结果，不含唯一后缀）'' AFTER `report_json_path`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'analysis_report'
      AND COLUMN_NAME = 'report_name'
);

PREPARE add_report_name_stmt FROM @add_report_name;
EXECUTE add_report_name_stmt;
DEALLOCATE PREPARE add_report_name_stmt;
