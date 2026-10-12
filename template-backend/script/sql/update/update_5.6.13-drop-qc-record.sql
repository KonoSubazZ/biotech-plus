-- ============================================================================
-- 老 qc_record 模块整体下线（2026-10-12）
-- 背景：湿实验质控改走 wet_lab_qc（business/wet-lab-qc.sql + POST /qc/wetLabQc/receive），
--       qc_record 表与它的 CRUD（QcRecordController / QcRecordMapper / 前端 qc-record-*）一起删除。
-- 本文件做三件事，可重复执行：
--   ① 删表 qc_record；
--   ② 清掉菜单里遗留的 qc:qcRecord:* 权限点行与它们的角色授权行（那套点位对应的模块已不存在）；
--   ③ 同步 qc_standard.qc_item 的列注释（原文里那句「与 qc_record.qc_item 对应」已失效）。
-- ⚠️ 删表不可逆：执行前先备份。本机这次执行的备份在
--    /home/liushangzhi/tmp_diag/qc_record_retire_20261012/（qc_record-files.tgz + qc_record-table.sql）
-- ★ 连带改动（已在代码里，别漏）：ruoyi-report 的「样本删除保护」原来 COUNT(*) FROM qc_record，
--    已改成读 wet_lab_qc（SampleInfoMapper.countWetLabQcBySubbarcode），否则删样本会 500。
-- ============================================================================

SET NAMES utf8mb4;

-- ① 删表
DROP TABLE IF EXISTS `qc_record`;

-- ② 菜单遗留权限点行 + 授权行（sys_role_menu 先删，避免留下指向已删菜单的授权；
--    sys_menu 的删除用自己过滤，不做自表子查询 —— MySQL 5.7 会报 1093）
DELETE FROM sys_role_menu
WHERE menu_id IN (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE perms LIKE 'qc:qcRecord:%') AS legacy);

DELETE FROM sys_menu WHERE perms LIKE 'qc:qcRecord:%';

-- ③ qc_item 列注释同步（原注释提到已下线的 qc_record）。注释里还有 qc_record 才改，可重复跑
SET @qc_item_comment = (SELECT column_comment FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'qc_standard' AND column_name = 'qc_item');
SET @ddl_comment = IF(@qc_item_comment LIKE '%qc_record%',
    'ALTER TABLE `qc_standard` MODIFY COLUMN `qc_item` varchar(100) NOT NULL COMMENT ''质控项目名称（中文展示名）''',
    'SELECT 1');
PREPARE stmt FROM @ddl_comment;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 核对：表没了、菜单里也不该再有 qc:qcRecord:*
SELECT COUNT(*) AS qc_record表还在 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'qc_record';
SELECT COUNT(*) AS 遗留qcRecord权限点 FROM sys_menu WHERE perms LIKE 'qc:qcRecord:%';
