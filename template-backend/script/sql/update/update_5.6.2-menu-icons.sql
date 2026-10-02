-- Run against the rbac-only database. Additive and repeatable.
--
-- 为什么需要这个脚本：
--   Ry_vue_5.X.sql 里的 sys_menu.icon 是 RuoYi 原生值（system / user / peoples / tree-table …），
--   那是「本地 svg 图标名」，但本模板前端（Soybean）的约定是**必须带前缀**：
--     template-frontend/src/store/modules/route/index.ts
--       if (route.meta.icon?.startsWith('local-icon-')) {
--         route.meta.localIcon = route.meta.icon.replace('local-icon-', 'menu-');
--         delete route.meta.icon;
--       } else if (!isNotNull(route.meta.icon)) {
--         route.meta.icon = defaultIcon;
--       }
--   即：只有 local-icon-<name> 会被映射成 src/assets/svg-icon/menu/<name>.svg；
--   其余值一律原样交给 iconify 的 <Icon>，而 iconify 里并不存在叫 system/user 的图标
--   → 菜单图标渲染为空白（同批菜单里只有 iconify 名如 mdi:xxx 的能显示）。
--
--   模板前端其实已经备好了全部图标文件（src/assets/svg-icon/menu/{system,user,peoples,
--   tree-table,dict,edit,message,log,upload,international,online,redis,dashboard,job,code,
--   form,logininfor,guide,monitor,tool}.svg），所以只差把 DB 的值加前缀。
--
-- 幂等：已带 local-icon- 前缀的不动；iconify 名（含 ':'）与占位 '#' 不动。
SET NAMES utf8mb4;

UPDATE sys_menu
SET icon = CONCAT('local-icon-', icon)
WHERE icon IS NOT NULL
  AND icon <> ''
  AND icon <> '#'
  AND icon NOT LIKE 'local-icon-%'
  AND icon NOT LIKE '%:%';

-- 核对（改完后可见菜单应当全部是 local-icon-* 或 iconify 名）
-- SELECT menu_id, menu_name, icon FROM sys_menu WHERE menu_type IN ('M','C') AND visible = '0' ORDER BY menu_id;
