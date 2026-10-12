-- ============================================================================
-- 质控标准（qc_standard）：新增「质控参数 qc_param」列
-- 口径：qc_item 放中文名（展示用），qc_param 放英文原始字段名（判定用）——
--       阈值判定是「标准项名 == 生信质控表列名」逐项比对（BioinfoQcServiceImpl.judgeStatus），
--       所以比对必须读 qc_param；字段名认不出的标准项不参与判定。
-- 存量数据：qc_param 直接沿用原 qc_item 的值（原来 qc_item 存的就是英文字段名），
--       回填后先保证判定口径不断，中文名由使用方在页面上按需改。
-- 幂等：MySQL 5.7 没有 ADD COLUMN IF NOT EXISTS，用 information_schema 判断后动态执行，可重复跑。
-- ============================================================================

SET @has_qc_param = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qc_standard' AND column_name = 'qc_param');
SET @ddl = IF(@has_qc_param = 0,
    'ALTER TABLE `qc_standard` ADD COLUMN `qc_param` varchar(100) DEFAULT NULL COMMENT ''质控参数（英文原始字段名，判定时与生信质控实际值比对；中文名见 qc_item）'' AFTER `qc_item`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 列注释同步：qc_item 现在放中文展示名（英文原始字段名挪到 qc_param）。注释里没有「中文展示名」才改，可重复跑
SET @qc_item_comment = (SELECT column_comment FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qc_standard' AND column_name = 'qc_item');
SET @ddl_item_comment = IF(@qc_item_comment NOT LIKE '%中文展示名%',
    'ALTER TABLE `qc_standard` MODIFY COLUMN `qc_item` varchar(100) NOT NULL COMMENT ''质控项目名称（中文展示名）''',
    'SELECT 1');
PREPARE stmt_item_comment FROM @ddl_item_comment;
EXECUTE stmt_item_comment;
DEALLOCATE PREPARE stmt_item_comment;

-- 存量回填：只在还没值时填，重复跑不会覆盖使用方后来在页面上改过的值
UPDATE `qc_standard`
SET `qc_param` = `qc_item`
WHERE `qc_param` IS NULL OR `qc_param` = '';
