-- =====================================================================================
-- analysis_report 重构 + sample_file 增加 project_no
--
-- 口径（用户 2026-10-22）：
--   1. analysis_report.product → product_bio，值与 analysis_data.product_bio 对齐（去 _shengyu 测试后缀）；
--   2. 删 product_desc（实测只有实体声明，无任何读写）；
--   3. 新增 analysis_batch_date（上传批次目录名快照，用于后台看报告表直接识别批次）；
--   4. 新增 project_no（项目编号，必须字段）—— 报告创建时按 subbarcode 查 sample_file 带过来；
--      为此 sample_file 先加 project_no（可空：老样本没有），录单/导入链路同步加字段；
--   5. report_sign_flow_no 加索引（签名回调按 flow_no 查，原来没索引）；
--   6. report_file_path 255 → 1000（与另外三个路径列一致，模板渲染的报告名会变长）。
--
-- [编] 标记 = 迁移补的编造值（不是业务实测值）：
--   · sample_file.project_no 老 34 行统一补 'NG0101'（= project_config 里现有的「圣域测试项目1」）
--   · analysis_report.analysis_batch_date 取 analysis_data.analysis_batch_date（老批次是 2026xxxx000000 这种补值）
--   · analysis_report.project_no 老 16 行同样补 'NG0101'
--
-- JSON：老报告的 JSON 文件不迁移（字段改名对 JSON 是破坏性更新），需要时重新生成报告即可。
-- 前置：mysqldump analysis_report / sample_file 到 /tmp
-- =====================================================================================

USE `ry-vue`;

-- -------------------------------------------------------------------------------------
-- 1. sample_file 加 project_no（可空；老样本补 [编] 值，否则现有样本无法进解读）
-- -------------------------------------------------------------------------------------
ALTER TABLE `sample_file`
    ADD COLUMN `project_no` VARCHAR(60) NULL
        COMMENT '项目编号（= project_config.project_no；录单/导入填）' AFTER `subbarcode`;

UPDATE `sample_file` SET `project_no` = 'NG0101' WHERE `project_no` IS NULL OR `project_no` = '';   -- [编]

-- -------------------------------------------------------------------------------------
-- 2. analysis_report：改名 + 加列 + 删列
-- -------------------------------------------------------------------------------------
ALTER TABLE `analysis_report`
    CHANGE COLUMN `product` `product_bio` VARCHAR(120) NOT NULL
        COMMENT '生信产品编码快照（= analysis_data.product_bio）',
    ADD COLUMN `analysis_batch_date` VARCHAR(14) NULL
        COMMENT '上传批次目录名快照（= analysis_data.analysis_batch_date）' AFTER `analysis_id`,
    ADD COLUMN `project_no` VARCHAR(60) NULL
        COMMENT '项目编号快照（= sample_file.project_no）' AFTER `product_id`,
    DROP COLUMN `product_desc`;

-- 老值去测试后缀：novopm2_blo_1238_shengyu → novopm2_blo_1238
UPDATE `analysis_report`
SET `product_bio` = SUBSTRING(`product_bio`, 1, CHAR_LENGTH(`product_bio`) - 8)
WHERE `product_bio` LIKE '%\_shengyu';

-- 补 product_id（按产品编码回查产品主数据）
UPDATE `analysis_report` r
    JOIN `product_config` p ON p.`product_bio` = r.`product_bio` AND p.`del_flag` = '0'
SET r.`product_id` = p.`id`
WHERE r.`product_id` IS NULL;

-- 补批次目录：优先取所属分析批次的真实值，取不到按 analysis_date + 000000 [编]
UPDATE `analysis_report` r
    JOIN `analysis_data` a ON a.`analysis_id` = r.`analysis_id`
SET r.`analysis_batch_date` = a.`analysis_batch_date`
WHERE r.`analysis_batch_date` IS NULL OR r.`analysis_batch_date` = '';

UPDATE `analysis_report`
SET `analysis_batch_date` = CONCAT(`analysis_date`, '000000')
WHERE `analysis_batch_date` IS NULL OR `analysis_batch_date` = '';   -- [编]

-- 补项目号：按 subbarcode 从 sample_file 取；取不到补 'NG0101' [编]
UPDATE `analysis_report` r
    JOIN `sample_file` s ON s.`subbarcode` = r.`subbarcode` AND s.`del_flag` = '0'
SET r.`project_no` = s.`project_no`
WHERE r.`project_no` IS NULL OR r.`project_no` = '';

UPDATE `analysis_report` SET `project_no` = 'NG0101' WHERE `project_no` IS NULL OR `project_no` = '';   -- [编]

-- 回填完收紧成必填
ALTER TABLE `analysis_report`
    MODIFY COLUMN `analysis_batch_date` VARCHAR(14) NOT NULL
        COMMENT '上传批次目录名快照（= analysis_data.analysis_batch_date）',
    MODIFY COLUMN `project_no` VARCHAR(60) NOT NULL
        COMMENT '项目编号快照（= sample_file.project_no）';

-- -------------------------------------------------------------------------------------
-- 3. 索引与长度
-- -------------------------------------------------------------------------------------
ALTER TABLE `analysis_report` ADD KEY `idx_report_sign` (`report_sign_flow_no`);

ALTER TABLE `analysis_report`
    MODIFY COLUMN `report_file_path` VARCHAR(1000) DEFAULT NULL COMMENT '报告路径';

-- -------------------------------------------------------------------------------------
-- 4. 核对
-- -------------------------------------------------------------------------------------
SELECT COUNT(*) AS report_rows,
       SUM(`project_no` IS NULL OR `project_no` = '') AS project_missing,
       SUM(`analysis_batch_date` IS NULL OR `analysis_batch_date` = '') AS batch_missing
FROM `analysis_report`;

SELECT `product_bio`, COUNT(*) FROM `analysis_report` GROUP BY `product_bio`;
SELECT `project_no`, COUNT(*) FROM `sample_file` WHERE `del_flag` = '0' GROUP BY `project_no`;

SHOW CREATE TABLE `analysis_report`;
