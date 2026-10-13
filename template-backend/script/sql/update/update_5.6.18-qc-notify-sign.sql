-- 质控不合格通知接上上签电子签署（2026-10-13）
--   ① qc_failed_notify 加两列：
--        sign_contract_id  送签后拿到的合同号（重复送签的判据）
--        signed_pdf_path   签名后 PDF（落 <root>/notify/signed/<yyyyMMdd>/，交付优先级 signed → pdf → docx）
--   ② 新增按钮点位 qc:notify:sign（送签 / 刷新签署状态）；生信与湿实验页共用（与 qc:notify:generate 同批）
-- 幂等：列用 information_schema 判存在后 PREPARE；菜单 NOT EXISTS；授权 INSERT IGNORE
-- 体检：python3 tools/check_db_schema.py
SET NAMES utf8mb4;

SET @col = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `qc_failed_notify` ADD COLUMN `sign_contract_id` varchar(64) DEFAULT NULL COMMENT ''上上签合同号（送签后回填）''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'qc_failed_notify' AND column_name = 'sign_contract_id');
PREPARE stmt FROM @col; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `qc_failed_notify` ADD COLUMN `signed_pdf_path` varchar(500) DEFAULT NULL COMMENT ''签名后PDF路径（上上签回传）''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE table_schema = DATABASE() AND table_name = 'qc_failed_notify' AND column_name = 'signed_pdf_path');
PREPARE stmt FROM @col; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 按钮点位（挂在生信质控菜单下；湿实验页复用同一批点位）
SET @menu_id = 1641;

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知送签', @menu_id, 7, '', NULL, 1, 0, 'F', '0', '0', 'qc:notify:sign', '#', 1, NOW(), '送签 / 刷新签署状态（上上签）'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu`
    WHERE parent_id = @menu_id AND perms = 'qc:notify:sign') AS c1);

-- 已授权「生成通知」的角色一并授权「通知送签」
INSERT IGNORE INTO `sys_role_menu` (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` g ON g.menu_id = rm.menu_id AND g.perms = 'qc:notify:generate'
JOIN `sys_menu` m ON m.parent_id = g.parent_id AND m.perms = 'qc:notify:sign';

SELECT column_name, column_type, column_comment FROM information_schema.COLUMNS
WHERE table_schema = DATABASE() AND table_name = 'qc_failed_notify'
  AND column_name IN ('sign_contract_id', 'signed_pdf_path');

SELECT menu_id, parent_id, menu_name, perms FROM `sys_menu` WHERE parent_id = 1641 ORDER BY order_num;
