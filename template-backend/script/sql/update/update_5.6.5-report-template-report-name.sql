-- 5.6.5 · report_template 增加「报告命名模板」列
--
-- 为什么：同一种报告要按模板定名字（如 圣域_{{template.customerCode}}_{{sampleInfo.sampleCode}}_{{reportId}}），
--         命名规则随模板配置走，所以列放在 report_template 上；
--         {{路径}} 的可用变量、校验与渲染见 org.dromara.report.service.ReportNameResolver / ReportNameCatalog。
-- 口径：静态文本逐字保留，{{路径}} 从渲染 JSON 与 template./report./now. 三个命名空间取值；空值 = 用默认命名。
-- 幂等：先查 information_schema，列已存在则跳过；可重复执行（重建库/回滚重跑）。
SET NAMES utf8mb4;

SET @add_report_name := (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `report_template` ADD COLUMN `report_name` VARCHAR(500) NULL DEFAULT NULL COMMENT ''报告命名模板：静态文本 + {{路径}} 动态取值；空=用默认命名'' AFTER `module_code`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'report_template'
      AND COLUMN_NAME = 'report_name'
);

PREPARE add_report_name_stmt FROM @add_report_name;
EXECUTE add_report_name_stmt;
DEALLOCATE PREPARE add_report_name_stmt;
