-- ============================================================================
-- 模板文件路径口径（2026-11-03）
--
-- 口径：模板文件 = 配置的模板根目录（report.renderer.templates-root）+ 本表的 template_path；
--       template_path 只存相对路径，可带子目录（如 xxx.docx / notify/xxx.docx），
--       绝对路径不入库（生产/测试根目录不同，写死绝对路径换环境就失效）。
--       渲染器以前忽略这一列、用 template_name + '.docx' 拼，本次改为读 template_path
--       （为空时才回退旧口径），所以这里只对齐注释，列与数据都不动。
--
-- 幂等：MODIFY COLUMN 本身幂等，可反复跑。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 biotech < update_5.6.52-report-template-path-comment.sql
-- ============================================================================
SET NAMES utf8mb4;

ALTER TABLE `report_template`
  MODIFY COLUMN `template_path` varchar(1000) NOT NULL
  COMMENT '模板文件相对路径（相对 report.renderer.templates-root，可带子目录，如 xxx.docx / notify/xxx.docx）';

SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_template' AND COLUMN_NAME = 'template_path';
