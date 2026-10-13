-- 清掉 RuoYi 基线带过来、本仓前端不存在的 7 条「死菜单」（menu_type='C'，component 指向不存在的视图）
-- 现象：前端登录/刷新时按 /getRouters 转路由，component 拼不出视图就抛
--       `View component "xxx" not found`（src/router/elegant/transform.ts:62，:134 捕获后 console.error），
--       该路由被整条丢弃 —— 界面表现为菜单点不开（Admin监控/任务调度中心 点进去是空白页）。
-- 依据（2026-10-13 逐条核对：DB 里 menu_type='C' 的行 ↔ src/views 下真实文件）：
--   116 修改生成配置   component=tool/gen/editTable     → 本仓是抽屉 tool/gen/modules/gen-table-operate-drawer.vue
--   117 Admin监控      component=monitor/admin/index    → 前端无此页；Spring Boot Admin 客户端本仓已关
--   120 任务调度中心   component=monitor/snailjob/index → 前端无此页；SnailJob 本仓已关
--   130 分配用户       component=system/role/authUser   → 本仓是抽屉 system/role/modules/role-auth-user-drawer.vue
--   131 分配角色       component=system/user/authRole   → 本仓是抽屉 system/user/modules/user-operate-drawer.vue
--   132 字典数据       component=system/dict/data       → 本仓同页实现 system/dict/modules/dict-data-*.vue
--   133 文件配置管理   component=system/oss/config      → 实际文件是 system/oss-config/index.vue（路径写错，见文末说明）
-- 权限点核对：删除不影响按钮权限 —— system:role:edit/user:edit/dict:list/ossConfig:list 另有 F 行承载
--             （1010 角色修改 / 1003 用户修改 / 105 字典管理 / 1620 配置列表）；
--             monitor:admin:list、monitor:snailjob:list 只有这两条承载，功能和前端都不存在，随菜单一并消失。
-- 幂等：DELETE 天然可重复执行；菜单/授权本来就不存在时影响 0 行。
-- 体检：python3 tools/check_db_schema.py
--
-- 已知未修（有意留痕，别当成本文件的问题）：「文件配置管理」页 system/oss-config/index.vue 目前**无入口可达** ——
--   文件管理页的「配置管理」按钮 push 的 route key 是 system_oss-config，而菜单 133 的 component 写成
--   system/oss/config（视图不存在）。删除前后实测都点不开，不是本次删除引入的。
--   要让它通：把菜单 component 改成 system/oss-config（与真实文件同名，路由名正好等于按钮的 key），
--   或把按钮改成跳已有路由；二选一，别两边都改。

SET NAMES utf8mb4;

-- 授权行先删（无外键，但先删子表语义更清楚）
DELETE FROM sys_role_menu WHERE menu_id IN (116, 117, 120, 130, 131, 132, 133);

-- 菜单行（这 7 条都没有子菜单，不用级联）
DELETE FROM sys_menu WHERE menu_id IN (116, 117, 120, 130, 131, 132, 133);
