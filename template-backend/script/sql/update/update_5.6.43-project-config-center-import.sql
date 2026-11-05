-- =====================================================================================
-- 研究中心批量导入 / 导出：按钮权限
--
-- 为什么（用户 2026-10-29）：项目配置里研究中心要能整表导出、改完再导回来。
--   口径：导出即模板；导入按「研究中心编号」匹配，对上改、对不上新增，永不删除；
--        先全量校验，任一行不通过就整体不导入。
-- 权限点：project:projectConfig:export / project:projectConfig:import（与 Controller 的 @SaCheckPermission 逐字一致）
-- 幂等：先查 sys_menu 再插，可重复执行。
-- =====================================================================================

USE `ry-vue`;
SET NAMES utf8mb4;

SET @menu_id = (SELECT menu_id FROM sys_menu WHERE path = 'project-config' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '导出', @menu_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'project:projectConfig:export', '#', 1, NOW(), '研究中心导出'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @menu_id AND perms = 'project:projectConfig:export') AS c1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '导入', @menu_id, 6, '', NULL, 1, 0, 'F', '0', '0', 'project:projectConfig:import', '#', 1, NOW(), '研究中心导入'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @menu_id AND perms = 'project:projectConfig:import') AS c2);

-- 超管拿到两个按钮权限（其它角色按需在「角色管理」里勾）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.parent_id = @menu_id AND m.perms IN ('project:projectConfig:export', 'project:projectConfig:import');

SELECT menu_id, menu_name, perms FROM sys_menu WHERE parent_id = @menu_id;
