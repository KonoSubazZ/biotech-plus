-- ============================================================================
-- 质控三个小改动（2026-10-12）
--   ① wet_lab_qc  加 status（质控状态：合格/不合格/待判定）与 notify_status（通知状态，暂不展示）
--   ② bioinfo_qc  加 notify_status（同上口径）
--   ③ qc_failed_notify 加 qc_category（区分这条通知是湿实验还是生信产生的）
--   ④ 「湿实验质控」菜单补一个按钮点位 qc:wetLabQc:edit（操作列新增「修改」）
-- 幂等：加列前先查 information_schema（MySQL 5.7 没有 ADD COLUMN IF NOT EXISTS，只有 MariaDB 有）；
--       菜单/授权沿用 NOT EXISTS / INSERT IGNORE。脚本可重复执行。
-- 配套：business/wet-lab-qc.sql、business/bioinfo-qc.sql、business/qc-failed-notify.sql、business/qc-menu.sql
-- 都是同一份定义（新装库用）。
-- ============================================================================

SET NAMES utf8mb4;

-- ① wet_lab_qc.status
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'wet_lab_qc' AND column_name = 'status') = 0,
    'ALTER TABLE `wet_lab_qc` ADD COLUMN `status` varchar(20) NOT NULL DEFAULT ''pending''
        COMMENT ''质控状态（passed 合格 / failed 不合格 / pending 待判定），服务端按质控标准判定'' AFTER `final_lib_conc_normal`',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ① wet_lab_qc.notify_status
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'wet_lab_qc' AND column_name = 'notify_status') = 0,
    'ALTER TABLE `wet_lab_qc` ADD COLUMN `notify_status` varchar(20) DEFAULT NULL
        COMMENT ''通知发送状态（已生成（文档）/ 已发送（邮件）），暂不展示''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ② bioinfo_qc.notify_status
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'bioinfo_qc' AND column_name = 'notify_status') = 0,
    'ALTER TABLE `bioinfo_qc` ADD COLUMN `notify_status` varchar(20) DEFAULT NULL
        COMMENT ''通知发送状态（已生成（文档）/ 已发送（邮件）），暂不展示''',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ③ qc_failed_notify.qc_category
SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'qc_failed_notify' AND column_name = 'qc_category') = 0,
    'ALTER TABLE `qc_failed_notify` ADD COLUMN `qc_category` varchar(20) DEFAULT NULL
        COMMENT ''质控类型（wet_lab 湿实验 / bioinfo 生信）—— 这条通知是在哪个质控页点处理产生的'' AFTER `bioinfo_qc_id`',
    'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ③b 存量记录回填：加这一列之前，「处理」只有生信页一个入口，所以历史记录都算 bioinfo。
--     幂等：只补 qc_category IS NULL 的行（湿实验的记录一定是新版本产生的，带 wet_lab_qc_id）。
UPDATE `qc_failed_notify` SET `qc_category` = 'bioinfo'
    WHERE `qc_category` IS NULL AND `wet_lab_qc_id` IS NULL;

-- ④ 「湿实验质控」菜单下补 qc:wetLabQc:edit（原模块只有查询/删除/接口接收）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', m.menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'qc:wetLabQc:edit', '#', 1, NOW(), '人工修正质控值'
FROM sys_menu m
WHERE m.component = 'qc/wet-lab/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = m.menu_id AND x.perms = 'qc:wetLabQc:edit');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM sys_role_menu rm
JOIN sys_menu m ON m.perms = 'qc:wetLabQc:edit'
WHERE rm.menu_id = (SELECT menu_id FROM sys_menu WHERE component = 'qc/wet-lab/index' LIMIT 1);

-- 核对
SELECT table_name, column_name, column_comment FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND ((table_name = 'wet_lab_qc' AND column_name IN ('status', 'notify_status'))
        OR (table_name = 'bioinfo_qc' AND column_name = 'notify_status')
        OR (table_name = 'qc_failed_notify' AND column_name = 'qc_category'))
    ORDER BY table_name, column_name;
SELECT menu_id, parent_id, menu_name, perms FROM sys_menu WHERE perms LIKE 'qc:wetLabQc:%' ORDER BY menu_id;
