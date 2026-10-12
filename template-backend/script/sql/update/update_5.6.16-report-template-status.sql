-- ============================================================================
-- report_template 状态值口径对齐（2026-10-12）
--   ① 状态取值从 ENABLED / DISABLED 固化为 active / inactive
--      （= 建表规范 ai-rules/04-db-schema.md §2.1「启用/停用固定 active/inactive」；
--        同库其它表 product_config / qc_standard 早就是这套值）
--   ② 列默认值 + 列注释一起改：默认 'active'，注释写清两个取值的含义
--   ③ 存量数据一并转换（含 del_flag='1' 的软删行，否则「删了再加」恢复出来的行还是旧值）
--
-- 幂等：UPDATE 用等值条件（第二次跑命中 0 行）；MODIFY COLUMN 不幂等，所以先查
--       information_schema.COLUMN_COMMENT，注释已经是新口径就跳过（MySQL 5.7 没有
--       ADD/MODIFY COLUMN IF NOT EXISTS，那是 MariaDB 的语法）。
-- 配套：template-backend/script/sql/business/report-interpretation.sql（新装库基线，同口径）
-- 影响面：报告模板的启用/停用；gen 模板查询 ReportTemplateMapper.selectEnabledByProductId 已同步改 status='active'
-- ============================================================================

SET NAMES utf8mb4;

-- ① 存量数据转换（5.7 的 UPDATE 不支持 LIMIT，这里也不需要）
UPDATE `report_template` SET `status` = 'active'   WHERE `status` = 'ENABLED';
UPDATE `report_template` SET `status` = 'inactive' WHERE `status` = 'DISABLED';

-- ② 列定义 + 注释：默认值 active，注释写明取值
SET @ddl_status = (SELECT IF(COLUMN_COMMENT NOT LIKE '%active 启用%',
    'ALTER TABLE `report_template` MODIFY COLUMN `status` varchar(20) NOT NULL DEFAULT ''active'' COMMENT ''状态（active 启用 / inactive 停用）''',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_template' AND COLUMN_NAME = 'status');
PREPARE stmt_status FROM @ddl_status;
EXECUTE stmt_status;
DEALLOCATE PREPARE stmt_status;

-- ③ 自检（应当只剩 active / inactive）
SELECT `status`, COUNT(*) AS rows_cnt FROM `report_template` GROUP BY `status`;
