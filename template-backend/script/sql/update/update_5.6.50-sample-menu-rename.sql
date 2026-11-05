-- ============================================================================
-- 5.6.50 样本管理菜单命名收敛
-- 背景：样本信息菜单从「报告管理」移到「样本管理」后，一级目录 path 仍是历史值 qc，
--       前端页面目录也还留在 views/report/sample-info，导致菜单 URL（/qc/sample-info）
--       与前端路由（/report/sample-info）不一致，直接访问旧地址会 403。
-- 口径：一级目录 path 统一为 sample；三个页面目录统一到 views/sample/*，
--       前端路由固定为 /sample/sample-info、/sample/wet-lab、/sample/bioinfo。
-- 说明：只改 sys_menu 的 path/component/menu_name；权限点（report:sampleInfo:*、qc:*）不变。
-- ============================================================================

SET NAMES utf8mb4;

-- 一级目录：兼容「已改过」和「还没改」两种状态，重复执行安全
SET @sample_menu_id = COALESCE(
    (SELECT menu_id FROM sys_menu
     WHERE parent_id = 0 AND path IN ('qc', 'sample') AND menu_name = '样本管理'
     LIMIT 1),
    (SELECT menu_id FROM sys_menu
     WHERE parent_id = 0 AND path = 'qc'
     LIMIT 1)
);

UPDATE sys_menu
SET path = 'sample', component = 'Layout', menu_type = 'M',
    update_by = 1, update_time = NOW()
WHERE menu_id = @sample_menu_id;

-- 两个质控页面：目录 qc/* -> sample/*
UPDATE sys_menu
SET component = 'sample/wet-lab/index', menu_name = 'route.sample_wet-lab',
    update_by = 1, update_time = NOW()
WHERE parent_id = @sample_menu_id AND path = 'wet-lab';

UPDATE sys_menu
SET component = 'sample/bioinfo/index', menu_name = 'route.sample_bioinfo',
    update_by = 1, update_time = NOW()
WHERE parent_id = @sample_menu_id AND path = 'bioinfo';

-- 样本信息页面：report/sample-info/index -> sample/sample-info/index
UPDATE sys_menu
SET component = 'sample/sample-info/index', menu_name = 'route.sample_sample-info',
    update_by = 1, update_time = NOW()
WHERE parent_id = @sample_menu_id AND path = 'sample-info'
  AND perms = 'report:sampleInfo:list';

-- 自检：应看到 样本管理(sample) + sample-info / wet-lab / bioinfo 三个子页面
SELECT menu_id, menu_name, parent_id, path, component, perms
FROM sys_menu
WHERE menu_id = @sample_menu_id OR parent_id = @sample_menu_id
ORDER BY parent_id, order_num;
