-- 「项目管理」菜单（menu_id=1625）
-- 来源：本地库里手工建的菜单，初始化脚本里没有 —— 固化下来，保证「从零重建」后菜单树完整。
-- 幂等：已存在则跳过。必须在 business/project.sql 之前执行（project.sql 靠 path='project' 定位父菜单）。

SET NAMES utf8mb4;

INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, query_param,
    is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
SELECT 1625, '项目管理', 0, 2, 'project', 'Layout', '', 1, 1, 'M', '0', '0', '', 'mdi:menu', 1, NOW(), 1, NOW(), ''
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu WHERE path = 'project' AND parent_id = 0) AS exists_check);

-- 授权给超级管理员角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE path = 'project' AND parent_id = 0;
