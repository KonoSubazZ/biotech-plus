-- 「报告管理」菜单与权限点位
-- 样本表 cms_sample 的结构已迁到 business/cms-sample.sql（药企 CMS 接口字段 + 公共字段）。
-- 本文件只保留报告模块的菜单/权限种子，不再建样本表。

SET NAMES utf8mb4;

-- ============================================================================
-- 2. 菜单：样本信息 —— 2026-10-21 从「报告管理」迁到「样本管理」（原质控管理）目录下，order=1
--    幂等：每段都用 NOT EXISTS 包一层派生表（MySQL 5.7 不支持相关派生表，用 @变量传值）
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告管理', 0, 3, 'report', NULL, 1, 0,
    'M', '0', '0', '', 'local-icon-post', 1, NOW(), '报告管理（目录）'
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = 0 AND path = 'report') AS exists_check);

SET @report_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);

-- 父目录是「样本管理」（一级目录 path='sample'），由 qc-menu.sql 建/改名，所以这里必须排在它之后执行
SET @qc_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'sample' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.sample_sample-info', @qc_id, 1, 'sample-info', 'sample/sample-info/index', 1, 0,
    'C', '0', '0', 'report:sampleInfo:list', 'local-icon-clipboard', 1, NOW(), '样本信息'
FROM (SELECT 1) AS dummy
WHERE @qc_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @qc_id AND path = 'sample-info') AS exists_check);

SET @menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @qc_id AND path = 'sample-info' LIMIT 1);


-- ============================================================================
-- 按钮权限（F 类型）—— 与 @SaCheckPermission 逐字一致
-- 导入单列一个点位（report:sampleInfo:import）：导入会覆盖既有样本，允许单独授权/收权
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:query') AS c1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:add') AS c2);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:edit') AS c3);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', @menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:remove', '#', 1, NOW(), '删除'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:remove') AS c4);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '导入', @menu_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:import', '#', 1, NOW(), '导入'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:import') AS c5);


-- ============================================================================
-- 4. 授权给超级管理员角色（sys_role_menu 主键 (role_id, menu_id)，INSERT IGNORE 保证幂等）
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @report_id OR m.menu_id = @menu_id OR m.parent_id = @menu_id;
