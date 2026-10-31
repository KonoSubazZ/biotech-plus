-- ----------------------------------------------------------------------------
-- 5.6.47：数据备份菜单的顺序与图标（纯菜单调整，不动表结构，可重复执行）
--
-- 1) 「数据备份」排到「诺禾致源官网」上面：官网 order 5 -> 6，数据备份 5
-- 2) 图标统一走本地 svg（local-icon-*）：carbon:data-backup 前端不认，渲染空白
--    新增 src/assets/svg-icon/menu/database.svg -> local-icon-database
-- ----------------------------------------------------------------------------
UPDATE `sys_menu` SET `order_num` = 6 WHERE `menu_id` = 4 AND `menu_name` = '诺禾致源官网';

-- 一级菜单：数据备份（挂在根下、path=backup）
UPDATE `sys_menu` SET `order_num` = 5, `icon` = 'local-icon-database'
WHERE `parent_id` = 0 AND `path` = 'backup' AND `menu_type` = 'M';

-- 子菜单图标（侧边栏二级不显示图标，但与菜单管理页的图标选择保持一致）
UPDATE `sys_menu` SET `icon` = 'local-icon-table'     WHERE `perms` = 'backup:db:list';
UPDATE `sys_menu` SET `icon` = 'local-icon-clipboard' WHERE `perms` = 'backup:sample:list';
