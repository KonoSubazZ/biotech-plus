-- =====================================================================================
-- 历史检出 / 历史匹配三张表增加 project_no（项目编号）
--
-- 口径（用户 2026-10-22）：
--   1. project_no 作为「项目归属」的展示字段，替代原来 customer（customer/client 列保留，仅作展示）；
--   2. 三张表：history_somatic、history_germline、report_variant_history；
--   3. 取值：提交审核时从 analysis_report.project_no 带下来（报告创建时按 subbarcode 取 sample_file.project_no）；
--   4. 报告 JSON（ReportTemplateData）顶层公共字段同步增加 projectNo。
--
-- [编]：老数据按 source_report_id / report_id 关联 analysis_report 回填；取不到的按 analysis_id 再取一次，
--       仍取不到的补 'NG0101'（现网唯一项目）。
-- =====================================================================================

USE `ry-vue`;

-- 1. 加列（先可空，回填后收紧）
ALTER TABLE `history_somatic`
    ADD COLUMN `project_no` VARCHAR(60) NULL
        COMMENT '项目编号（= analysis_report.project_no；替代 customer 做归属展示，customer 列保留）' AFTER `customer`;

ALTER TABLE `history_germline`
    ADD COLUMN `project_no` VARCHAR(60) NULL
        COMMENT '项目编号（= analysis_report.project_no；替代 customer 做归属展示，customer 列保留）' AFTER `customer`;

ALTER TABLE `report_variant_history`
    ADD COLUMN `project_no` VARCHAR(60) NULL
        COMMENT '项目编号（= analysis_report.project_no；替代 client 做归属展示，client 列保留）' AFTER `client`;

-- 2. 回填：优先按报告回填，其次按分析批次取该批次最新报告，最后 [编] 兜底
UPDATE `history_somatic` h
    JOIN `analysis_report` r ON r.`report_id` = h.`source_report_id` AND r.`del_flag` = '0'
SET h.`project_no` = r.`project_no`
WHERE h.`project_no` IS NULL OR h.`project_no` = '';

UPDATE `history_germline` h
    JOIN `analysis_report` r ON r.`report_id` = h.`source_report_id` AND r.`del_flag` = '0'
SET h.`project_no` = r.`project_no`
WHERE h.`project_no` IS NULL OR h.`project_no` = '';

UPDATE `report_variant_history` h
    JOIN `analysis_report` r ON r.`report_id` = h.`report_id` AND r.`del_flag` = '0'
SET h.`project_no` = r.`project_no`
WHERE h.`project_no` IS NULL OR h.`project_no` = '';

UPDATE `history_somatic` h
SET h.`project_no` = (SELECT r.`project_no` FROM `analysis_report` r
                      WHERE r.`analysis_id` = h.`source_analysis_id` AND r.`del_flag` = '0'
                      ORDER BY r.`report_id` DESC LIMIT 1)
WHERE h.`project_no` IS NULL OR h.`project_no` = '';

UPDATE `history_germline` h
SET h.`project_no` = (SELECT r.`project_no` FROM `analysis_report` r
                      WHERE r.`analysis_id` = h.`source_analysis_id` AND r.`del_flag` = '0'
                      ORDER BY r.`report_id` DESC LIMIT 1)
WHERE h.`project_no` IS NULL OR h.`project_no` = '';

UPDATE `history_somatic` SET `project_no` = 'NG0101' WHERE `project_no` IS NULL OR `project_no` = '';   -- [编]
UPDATE `history_germline` SET `project_no` = 'NG0101' WHERE `project_no` IS NULL OR `project_no` = '';  -- [编]
UPDATE `report_variant_history` SET `project_no` = 'NG0101'
WHERE `project_no` IS NULL OR `project_no` = '';                                                       -- [编]

-- 3. 收紧成必填 + 检索索引（历史检出页按项目编号筛选）
ALTER TABLE `history_somatic`
    MODIFY COLUMN `project_no` VARCHAR(60) NOT NULL
        COMMENT '项目编号（= analysis_report.project_no；替代 customer 做归属展示，customer 列保留）';

ALTER TABLE `history_germline`
    MODIFY COLUMN `project_no` VARCHAR(60) NOT NULL
        COMMENT '项目编号（= analysis_report.project_no；替代 customer 做归属展示，customer 列保留）';

ALTER TABLE `report_variant_history`
    MODIFY COLUMN `project_no` VARCHAR(60) NOT NULL
        COMMENT '项目编号（= analysis_report.project_no；替代 client 做归属展示，client 列保留）',
    ADD KEY `idx_rvh_project` (`project_no`, `report_generated_at`);

-- 4. 核对
SELECT 'history_somatic' AS t, COUNT(*) total, SUM(`project_no` = 'NG0101') fallback FROM `history_somatic`
UNION ALL
SELECT 'history_germline', COUNT(*), SUM(`project_no` = 'NG0101') FROM `history_germline`
UNION ALL
SELECT 'report_variant_history', COUNT(*), SUM(`project_no` = 'NG0101') FROM `report_variant_history`;
