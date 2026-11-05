-- ============================================================================
-- 产品配置字段重构（2026-11-03 口径定稿）
--
-- 背景：产品配置图（product_config.png）新增了一批产品主数据字段，product_config
-- 只有 code/bio/name/test_type/panel_type/related_diseases/report_cycle_days，
-- 与配置图差了 8 列。本次补齐，并对齐配置图的列顺序与中文口径：
--   新增 trade_name          商品名（诺卓康）
--   新增 control_type        对照（血液 / PBMC / 无）
--   新增 product_version     版本（LDT / IVD）
--   新增 sequencing_platform 测序平台（surf5000 Dx）
--   新增 gene_count          基因数/DNA（1238）
--   新增 maintain_lab        维护实验室（天津实验室）
--   新增 related_process     关联工序（全流程）
--   新增 probe_type          探针类型（正常1238探针 / 正常1238探针+UMI）
--   改注释 related_diseases  相关疾病 → 适应症（列名不动，数据不动）
--   改注释 test_type         检测类型 → 样本类型（列名不动，数据不动）
--   新增索引 idx_product_bio 驱动/报告按生信编码反查产品，原先无索引
--
-- 按口径不做的：
--   · 样本取材类型（全血（白细胞）/石蜡切片）—— 本次不要
--   · 去dup前测序深度、数据量 —— 本次不要
--   · 检测方式 —— 不入库，业务默认「来样即检」
--   · 报告周期 —— 保留原 report_cycle_days int(天)，不换文案型
--   · panel_type 图里没有，按「先冗余上」保留
--
-- 幂等：每条先查 information_schema 再决定执行什么，可反复跑。
--       不用 ADD COLUMN IF NOT EXISTS（MariaDB 语法，MySQL 5.7 不认）。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 ry-vue < update_5.6.51-product-config-refactor.sql
-- ============================================================================

SET NAMES utf8mb4;

-- ① trade_name 商品名
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `trade_name` varchar(120) NULL COMMENT ''商品名'' AFTER `product_code`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'trade_name');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ② control_type 对照
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `control_type` varchar(60) NULL COMMENT ''对照（血液 / PBMC / 无）'' AFTER `test_type`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'control_type');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ③ product_version 版本
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `product_version` varchar(30) NULL COMMENT ''版本（LDT / IVD）'' AFTER `control_type`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'product_version');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ④ sequencing_platform 测序平台
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `sequencing_platform` varchar(60) NULL COMMENT ''测序平台（surf5000 Dx 等）'' AFTER `product_version`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'sequencing_platform');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑤ gene_count 基因数/DNA
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `gene_count` int(11) NULL COMMENT ''基因数/DNA（panel 基因个数，如 1238）'' AFTER `sequencing_platform`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'gene_count');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑥ maintain_lab 维护实验室
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `maintain_lab` varchar(60) NULL COMMENT ''维护实验室'' AFTER `report_cycle_days`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'maintain_lab');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑦ related_process 关联工序
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `related_process` varchar(100) NULL COMMENT ''关联工序（如 全流程）'' AFTER `maintain_lab`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'related_process');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑧ probe_type 探针类型
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `probe_type` varchar(120) NULL COMMENT ''探针类型（如 正常1238探针 / 正常1238探针+UMI）'' AFTER `related_process`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'probe_type');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑨ 物理列序对齐配置图：适应症 → 样本类型 → 对照 → 版本 → 测序平台 → 基因数
--    （MODIFY ... AFTER 天然幂等；8 行小表，重复执行无代价）
ALTER TABLE `product_config` MODIFY COLUMN `product_name` varchar(120) NOT NULL COMMENT '产品名称（系统用）' AFTER `product_bio`;
ALTER TABLE `product_config` MODIFY COLUMN `related_diseases` varchar(500) NULL COMMENT '适应症' AFTER `product_name`;
ALTER TABLE `product_config` MODIFY COLUMN `test_type` varchar(60) NULL COMMENT '样本类型（组织 / 血液）' AFTER `related_diseases`;

-- ⑩ panel_type 图里没有，冗余保留，挪到业务字段最后
ALTER TABLE `product_config` MODIFY COLUMN `panel_type` varchar(60) NULL COMMENT 'panel类型（DNA / RNA / DNA_RNA / WES / HRD 等）' AFTER `probe_type`;

-- ⑪ idx_product_bio：驱动/报告按生信编码反查产品
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD KEY `idx_product_bio` (`tenant_id`,`product_bio`)',
        'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND INDEX_NAME = 'idx_product_bio');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 校验：列顺序 + 索引
SELECT ORDINAL_POSITION, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config'
ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config'
GROUP BY INDEX_NAME;
