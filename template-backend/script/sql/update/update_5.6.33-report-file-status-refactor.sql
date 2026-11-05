-- =====================================================================================
-- data_file_status → analysis_file_status：表名对齐 analysis_data / analysis_report
--
-- 口径（用户 2026-10-22）：
--   1. 表名 analysis_file_status（与 analysis_data 同前缀）；用 RENAME 保数据与自增，不重建；
--   2. 归属只认 analysis_id；只保留两列展示冗余：analysis_date（Tab② 分析日期列在用）、
--      subbarcode（后台按样本手工导出用），另加 product_bio（后台看表/导出直接可读，A 方案）；
--   3. 索引收敛：idx_file_analysis 扩成 (analysis_id, file_name)（驱动按批次+文件名取最新行/置删）；
--      删掉两个没有查询使用的索引 idx_file_path_name、idx_report_file_subbarcode；
--   4. 新增 file_size（字节数，识别 0 字节/传输截断）；老数据留 NULL，可选脚本回填；
--   5. 不建唯一键（同一文件重驱保留 del_flag=1 的历史行）。
--
-- 前置：mysqldump analysis_file_status（原 data_file_status）与 analysis_data 到 /tmp
-- 执行后必须同步改代码：3 个 XML（drive/DriveFileStatusMapper、report/InterpretationMapper、
--   report/ReportRevisionMapper）+ 注释里的旧表名，否则文件页签/重驱/报告修订会 500。
-- =====================================================================================

USE `ry-vue`;

-- -------------------------------------------------------------------------------------
-- 1. 改名（保数据、保 AUTO_INCREMENT）
-- -------------------------------------------------------------------------------------
RENAME TABLE `data_file_status` TO `analysis_file_status`;

-- -------------------------------------------------------------------------------------
-- 2. 加列：product_bio（先允许 NULL 便于回填）、file_size
-- -------------------------------------------------------------------------------------
ALTER TABLE `analysis_file_status`
    ADD COLUMN `product_bio` VARCHAR(120) NULL
        COMMENT '生信产品编码（冗余展示，= analysis_data.product_bio）' AFTER `subbarcode`,
    ADD COLUMN `file_size` BIGINT NULL
        COMMENT '文件字节数（0 = 空文件）' AFTER `file_text`;

-- 老行按 analysis_id 从 analysis_data 回填产品编码
UPDATE `analysis_file_status` f
    JOIN `analysis_data` a ON a.`analysis_id` = f.`analysis_id`
SET f.`product_bio` = a.`product_bio`
WHERE f.`product_bio` IS NULL OR f.`product_bio` = '';

ALTER TABLE `analysis_file_status`
    MODIFY COLUMN `product_bio` VARCHAR(120) NOT NULL
        COMMENT '生信产品编码（冗余展示，= analysis_data.product_bio）';

-- -------------------------------------------------------------------------------------
-- 3. 索引收敛
-- -------------------------------------------------------------------------------------
ALTER TABLE `analysis_file_status` DROP INDEX `idx_file_path_name`;
ALTER TABLE `analysis_file_status` DROP INDEX `idx_report_file_subbarcode`;
ALTER TABLE `analysis_file_status` DROP INDEX `idx_file_analysis`;
ALTER TABLE `analysis_file_status` ADD KEY `idx_file_analysis` (`analysis_id`, `file_name`);

-- -------------------------------------------------------------------------------------
-- 4. 核对（执行后人工看）
-- -------------------------------------------------------------------------------------
SELECT COUNT(*) AS rows_all,
       SUM(`product_bio` IS NULL OR `product_bio` = '') AS bio_missing,
       SUM(`file_size` IS NULL) AS size_missing
FROM `analysis_file_status`;

-- 孤儿行：file 行指向了不存在的批次（应为 0）
SELECT COUNT(*) AS orphan_rows
FROM `analysis_file_status` f
         LEFT JOIN `analysis_data` a ON a.`analysis_id` = f.`analysis_id`
WHERE a.`analysis_id` IS NULL;

SHOW CREATE TABLE `analysis_file_status`;
