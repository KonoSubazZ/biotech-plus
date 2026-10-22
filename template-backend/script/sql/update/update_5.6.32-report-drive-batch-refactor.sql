-- =====================================================================================
-- 驱动入库重构：上传批次目录（14 位 analysis_batch_date）取代 batch_no
--
-- 背景（用户 2026-10-22 口径，替代 update_5.6.31 的 batch_no 设计）：
--   1. 原始上传路径 = <root>/<analysis_batch_date>/<product_bio>/<subbarcode>/<subbarcode>T/10.Upload/
--      analysis_batch_date = 上游一次上传的目录名（YYYYMMDDHHMMSS），就是批次身份；
--   2. analysis_date = LEFT(analysis_batch_date, 8)，只做页面展示与按天搜索；
--   3. batch_no 没有业务作用，删除；「同批次已驱动过」由接口按三件套（batch_date + product_bio + subbarcode）
--      判重后拒绝，「重新解读」不判重、直接新建 analysis_id；
--   4. barcode / analyzer 在驱动侧不再写（barcode≡subbarcode、analyzer 由解读环节维护），
--      report 侧 SQL 用 d.subbarcode AS barcode 顶替，前端零改动；
--   5. data_file_status 去掉快照列 product_name / barcode / analyzer，file_path 加长到 512。
--
-- 老数据回填说明（[编] = 编造值，不是实测）：
--   analysis_batch_date = analysis_date + '000000'  [编]（老数据没有批次目录，只能按当天 0 点补）
--   product 值去掉 _shengyu 后缀 → 对齐 product_config.product_bio（测试租户后缀）
--
-- 前置：mysqldump analysis_data data_file_status 到 /tmp（改前已备份）
-- =====================================================================================

USE `ry-vue`;

-- -------------------------------------------------------------------------------------
-- 1. analysis_data：批次身份换成 analysis_batch_date，去掉 batch_no
-- -------------------------------------------------------------------------------------
ALTER TABLE `analysis_data`
    ADD COLUMN `analysis_batch_date` VARCHAR(14) NULL
        COMMENT '上传批次目录名 YYYYMMDDHHMMSS（批次身份）' AFTER `analysis_id`;

-- [编] 老数据没有批次目录：补成当天 00:00:00
UPDATE `analysis_data`
SET `analysis_batch_date` = CONCAT(`analysis_date`, '000000')
WHERE `analysis_batch_date` IS NULL OR `analysis_batch_date` = '';

ALTER TABLE `analysis_data`
    MODIFY COLUMN `analysis_batch_date` VARCHAR(14) NOT NULL
        COMMENT '上传批次目录名 YYYYMMDDHHMMSS（批次身份）';

-- 旧索引依赖 product / batch_no，先删
ALTER TABLE `analysis_data` DROP INDEX `uk_analysis_batch`;
ALTER TABLE `analysis_data` DROP INDEX `idx_analysis_date_product`;
ALTER TABLE `analysis_data` DROP INDEX `idx_analysis_product`;

-- product → product_bio（值域改成 product_config.product_bio，即上传目录第 2 层）
ALTER TABLE `analysis_data`
    CHANGE COLUMN `product` `product_bio` VARCHAR(120) NOT NULL
        COMMENT '生信产品编码（= 上传目录第 2 层 = product_config.product_bio）';

-- 老值去测试后缀：novopm2_blo_1238_shengyu → novopm2_blo_1238
UPDATE `analysis_data`
SET `product_bio` = SUBSTRING(`product_bio`, 1, CHAR_LENGTH(`product_bio`) - 8)
WHERE `product_bio` LIKE '%\_shengyu';

-- 驱动侧不再写这两列（barcode≡subbarcode、analyzer 归解读环节）
ALTER TABLE `analysis_data` DROP COLUMN `batch_no`;
ALTER TABLE `analysis_data` DROP COLUMN `barcode`;
ALTER TABLE `analysis_data` DROP COLUMN `analyzer`;

-- 判重查询 / 批次定位用；不加唯一键：重新解读要允许三件套相同、analysis_id 不同的多行
ALTER TABLE `analysis_data`
    ADD KEY `idx_analysis_batch` (`analysis_batch_date`, `product_bio`, `subbarcode`),
    ADD KEY `idx_analysis_date_product` (`analysis_date`, `product_bio`),
    ADD KEY `idx_analysis_subbarcode` (`subbarcode`);

-- 顺手补齐 product_id（历史行按 product_bio 回填）
UPDATE `analysis_data` a
    JOIN `product_config` p ON p.`product_bio` = a.`product_bio` AND p.`del_flag` = '0'
SET a.`product_id` = p.`id`
WHERE a.`product_id` IS NULL;

-- -------------------------------------------------------------------------------------
-- 2. data_file_status：去掉快照列，file_path 加长
-- -------------------------------------------------------------------------------------
ALTER TABLE `data_file_status`
    MODIFY COLUMN `file_path` VARCHAR(512) NOT NULL COMMENT '文件绝对路径';

ALTER TABLE `data_file_status` DROP INDEX `idx_report_case`;

ALTER TABLE `data_file_status`
    DROP COLUMN `product_name`,
    DROP COLUMN `barcode`,
    DROP COLUMN `analyzer`;

-- -------------------------------------------------------------------------------------
-- 3. 权限点：删掉「驱动-按日期 / 驱动-按产品」两个已下线接口的权限与角色关联
-- -------------------------------------------------------------------------------------
DELETE rm FROM `sys_role_menu` rm
    JOIN `sys_menu` m ON m.`menu_id` = rm.`menu_id`
WHERE m.`perms` IN ('report:drive:date', 'report:drive:product');

DELETE FROM `sys_menu` WHERE `perms` IN ('report:drive:date', 'report:drive:product');

-- -------------------------------------------------------------------------------------
-- 4. 核对（执行后人工看结果）
-- -------------------------------------------------------------------------------------
SELECT COUNT(*) AS analysis_rows FROM `analysis_data`;
SELECT COUNT(*) AS file_rows FROM `data_file_status`;
SELECT `product_bio`, COUNT(*) FROM `analysis_data` GROUP BY `product_bio`;
SELECT `analysis_batch_date`, `analysis_date`, `product_bio`, `subbarcode`, `drive_status`
FROM `analysis_data` ORDER BY `analysis_id` DESC LIMIT 5;
