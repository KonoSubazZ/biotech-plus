-- =====================================================================================
-- 驱动/报告域重构后的列注释对齐（无结构变化）
--
-- 背景：三张表已重构完成（analysis_data / analysis_file_status / analysis_report），
-- 但 product_config.product_bio 的列注释还写着「与 analysis_data.product 逐字相等」，
-- analysis_data 的两个派生/来源列也缺说明，容易让后来人按旧列名写 SQL。
-- =====================================================================================

USE `ry-vue`;

ALTER TABLE `product_config`
    MODIFY COLUMN `product_bio` VARCHAR(120) NOT NULL
        COMMENT '生信编码（= 上传目录第 2 层 product，与 analysis_data.product_bio 逐字相等）';

ALTER TABLE `analysis_data`
    MODIFY COLUMN `analysis_date` VARCHAR(8) NOT NULL
        COMMENT '分析日期YYYYMMDD（= LEFT(analysis_batch_date,8)，只做展示与按天搜索）',
    MODIFY COLUMN `subbarcode` VARCHAR(80) NOT NULL
        COMMENT '样本编号（上传目录第 3 层，不含结尾 T）';

ALTER TABLE `analysis_file_status`
    MODIFY COLUMN `analysis_date` VARCHAR(8) NOT NULL
        COMMENT '冗余展示（来自所属批次的 analysis_data.analysis_date）',
    MODIFY COLUMN `subbarcode` VARCHAR(80) NOT NULL
        COMMENT '冗余展示（后台按样本导出用，不参与查询）';

SELECT COLUMN_NAME, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'ry-vue'
  AND ((TABLE_NAME = 'product_config' AND COLUMN_NAME = 'product_bio')
    OR (TABLE_NAME = 'analysis_data' AND COLUMN_NAME IN ('analysis_date', 'subbarcode'))
    OR (TABLE_NAME = 'analysis_file_status' AND COLUMN_NAME IN ('analysis_date', 'subbarcode')));
