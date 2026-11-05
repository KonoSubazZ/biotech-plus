-- ============================================================================
-- 产品配置列表重排（2026-10-22 口径定稿）
--
-- 背景：product_config 原来只有一个 name 列，同时被当成「生信编码」和「对外产品名」用：
--   · DriveScopeBo 注释要求上传目录第 2 层 product「必须逐字等于 product_config.name」
--   · 报告/解读侧拿 analysis_data.product（如 novopm2_tis_1238_shengyu）按 name 反查 product_id
--   · 页面/报告模板下拉又把 name 当中文展示名
-- 现在拆成两列，并对齐对外编码命名：
--   code         → product_code （改名，保留数据与唯一键）
--   name         → 删除（旧值口径不清，按用户决定不迁数据）
--   product_bio   新增：生信编码，逐字等于上传目录第 2 层 / analysis_data.product
--   product_name  新增：对外展示名（页面、报告模板下拉）
--   panel_type    新增：panel类型（DNA / RNA / DNA_RNA / WES / HRD …，口径同生信质检判定）
-- 保留不动：test_type（组织/血液）、related_diseases、report_cycle_days、status、remark、公共字段
--
-- 幂等：每条都先查 information_schema 再决定执行什么，可反复跑（第二次只剩 SELECT 1）。
--       不用 ADD COLUMN IF NOT EXISTS —— 那是 MariaDB 语法，MySQL 5.7 不认。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 ry-vue < update_5.6.32-product-config-columns.sql
-- ============================================================================

SET NAMES utf8mb4;

-- ① code → product_code（CHANGE COLUMN 同时改名改定义，数据与索引都跟着走）
SET @ddl = (SELECT IF(COUNT(*) = 0, 'SELECT 1',
        'ALTER TABLE `product_config` CHANGE COLUMN `code` `product_code` varchar(60) NOT NULL COMMENT ''产品编码''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'code');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ② 删除旧 name 列（存量 4 行是本地测试数据，按口径不迁）
SET @ddl = (SELECT IF(COUNT(*) = 0, 'SELECT 1',
        'ALTER TABLE `product_config` DROP COLUMN `name`')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'name');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ③ 新增 product_bio（先允许 NULL，回填后再收紧，否则存量行会卡住 ALTER）
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `product_bio` varchar(120) NULL COMMENT ''生信编码（= 上传目录第 2 层 product，与 analysis_data.product 逐字相等）'' AFTER `product_code`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'product_bio');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ④ 新增 product_name
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `product_name` varchar(120) NULL COMMENT ''产品名称（对外展示、报告用）'' AFTER `product_bio`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'product_name');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑤ 新增 panel_type（放在 test_type 之后，和页面表单顺序一致）
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `product_config` ADD COLUMN `panel_type` varchar(60) NULL COMMENT ''panel类型（DNA / RNA / DNA_RNA / WES / HRD 等，口径同生信质检判定）'' AFTER `test_type`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'panel_type');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑥ 存量行回填：两列都用 product_code 占位（本地只有 4 条测试产品，页面上手工改成真实中文名/生信名即可）
UPDATE `product_config` SET `product_bio` = `product_code` WHERE `product_bio` IS NULL;
UPDATE `product_config` SET `product_name` = `product_code` WHERE `product_name` IS NULL;

-- ⑦ 收紧为 NOT NULL（已经收紧过就跳过）
SET @ddl = (SELECT IF(COUNT(*) = 0, 'SELECT 1',
        'ALTER TABLE `product_config` MODIFY COLUMN `product_bio` varchar(120) NOT NULL COMMENT ''生信编码（= 上传目录第 2 层 product，与 analysis_data.product 逐字相等）''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config'
      AND COLUMN_NAME = 'product_bio' AND IS_NULLABLE = 'YES');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0, 'SELECT 1',
        'ALTER TABLE `product_config` MODIFY COLUMN `product_name` varchar(120) NOT NULL COMMENT ''产品名称（对外展示、报告用）''')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config'
      AND COLUMN_NAME = 'product_name' AND IS_NULLABLE = 'YES');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑧ 唯一键改名（CHANGE COLUMN 会把索引带过去，但索引名还是旧的 *_code）
SET @ddl = (SELECT IF(COUNT(*) = 0, 'SELECT 1',
        'ALTER TABLE `product_config` RENAME INDEX `uk_product_config_code` TO `uk_product_config_product_code`')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config'
      AND INDEX_NAME = 'uk_product_config_code');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑨ 对齐 test_type 注释口径（口径变了注释要跟着变；已是新注释就跳过）
SET @cmt = (SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'test_type');
SET @ddl = IF(@cmt IS NOT NULL AND @cmt NOT LIKE '%组织%',
        'ALTER TABLE `product_config` MODIFY COLUMN `test_type` varchar(60) NULL COMMENT ''检测类型（组织 / 血液）''',
        'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ⑩ 对齐 product_bio / panel_type 注释（2026-10-22 二次定稿：页面文案叫「生信编码」「panel类型」）
SET @cmt = (SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'product_bio');
SET @ddl = IF(@cmt IS NOT NULL AND @cmt NOT LIKE '%生信编码%',
        'ALTER TABLE `product_config` MODIFY COLUMN `product_bio` varchar(120) NOT NULL COMMENT ''生信编码（= 上传目录第 2 层 product，与 analysis_data.product 逐字相等）''',
        'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @cmt = (SELECT COLUMN_COMMENT FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_config' AND COLUMN_NAME = 'panel_type');
SET @ddl = IF(@cmt IS NOT NULL AND @cmt NOT LIKE '%panel类型%',
        'ALTER TABLE `product_config` MODIFY COLUMN `panel_type` varchar(60) NULL COMMENT ''panel类型（DNA / RNA / DNA_RNA / WES / HRD 等，口径同生信质检判定）''',
        'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
