-- ============================================================================
-- cms_sample 增加 sync_log：最近一次 CMS 同步 / 行内刷新的变更明细
--
-- 结构：JSON 数组（一条变更一个元素，与 CmsFieldChangeVo 字段一致）
--   [{"field":"sampleType","label":"样本类型","oldValue":"石蜡切片","newValue":"全血(白细胞)",
--     "by":"liushangzhi","at":"2026-11-03 18:20:00"}]
--   新增样本时只有一条：field="__insert__"、label="新增样本"、oldValue=null、newValue=样本编号
--
-- 口径：只留最近一次，不建明细表；更早的操作轨迹看 sys_oper_log（refresh/sync 两个接口都带 @Log）。
-- 位置：del_flag 之后、tenant_id 之前（公共字段区末尾），不打乱 45 个业务列的列序
--       （Excel 导入按列序读，业务列顺序不能变）。
-- 幂等：先查 information_schema 再决定执行什么，可反复跑。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 biotech < update_5.6.54-cms-sample-sync-log.sql
-- ============================================================================

SET NAMES utf8mb4;

SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `cms_sample` ADD COLUMN `sync_log` text COMMENT ''最近一次同步/刷新的变更明细（JSON 数组：field/label/oldValue/newValue/by/at）'' AFTER `del_flag`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_sample' AND COLUMN_NAME = 'sync_log');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
