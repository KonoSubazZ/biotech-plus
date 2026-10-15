-- 报告解读：页签级权限（6 个页签各一个按钮权限点；接口与页签用同一个权限点）
-- ============================================================================
-- 背景：解读详情页 6 个页签原来共用「按动作」的权限点（query / edit / preview / review / send /
--       generate），角色权限树里看不出「哪个权限点对应哪个页签」，也没法只放行某一个页签。
-- 本次口径（与 InterpretationController 的 @SaCheckPermission 逐字一致）：
--       页签① LIMS 信息 = report:interpretation:lims     （新增）
--       页签② 集群对接 = report:interpretation:files    （新增）
--       页签③ 筛选位点 = report:interpretation:variants （新增）
--       页签④ 报告预览 = report:interpretation:preview  （复用）
--       页签⑤ 审核报告 = report:interpretation:review   （复用）
--       页签⑥ 报告发送 = report:interpretation:send     （复用）
--       列表页「解读」按钮 = report:interpretation:enter（不变）
-- 「有页签权限 = 页签内按钮与接口都能用」→ 退役 :query / :edit / :generate 三个动作权限点。
-- 授权迁移：谁原来有 :query 就补 ①②③、有 :edit 就补 ③④、有 :generate 就补 ④，角色可访问范围不缩水。
-- 幂等：新增按 perms 判 NOT EXISTS；改名/排序 UPDATE 可重复执行；授权 INSERT IGNORE；删除天然幂等。
-- ============================================================================
SET NAMES utf8mb4;

SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);
SET @menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @report_menu_id AND path = 'interpretation' AND menu_type = 'C' LIMIT 1);

-- 1. 新增 3 个页签按钮（F 型，不进侧边栏；icon 用 '#' 与既有按钮一致）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT t.menu_name, @menu_id, t.order_num, '', NULL, 1, 0, 'F', '0', '0', t.perms, '#', 1, NOW(), t.remark
FROM (SELECT 'LIMS 信息' AS menu_name, 2 AS order_num, 'report:interpretation:lims' AS perms,
             '页签①LIMS 信息（仅查看：患者/疾病/样本）' AS remark
      UNION ALL SELECT '集群对接', 3, 'report:interpretation:files', '页签②集群对接（仅查看下机文件与文件内容）'
      UNION ALL SELECT '筛选位点', 4, 'report:interpretation:variants', '页签③筛选位点（入报告开关/过滤理由）') AS t
WHERE @menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = @menu_id AND x.perms = t.perms);

-- 2. 复用的 3 个按钮改成页签名、排进页签顺序（列表页「解读」按钮固定 order_num=1）
UPDATE sys_menu SET menu_name = '进入解读', order_num = 1
 WHERE parent_id = @menu_id AND perms = 'report:interpretation:enter';

UPDATE sys_menu SET menu_name = '报告预览', order_num = 5,
       remark = '页签④报告预览（预览/改靶/临床意义/生成/下载/提交审核）'
 WHERE parent_id = @menu_id AND perms = 'report:interpretation:preview';

UPDATE sys_menu SET menu_name = '审核报告', order_num = 6, remark = '页签⑤审核报告（审核通过/驳回）'
 WHERE parent_id = @menu_id AND perms = 'report:interpretation:review';

UPDATE sys_menu SET menu_name = '报告发送', order_num = 7, remark = '页签⑥报告发送（邮件发送）'
 WHERE parent_id = @menu_id AND perms = 'report:interpretation:send';

-- 3. 授权迁移：旧权限点的角色授权按覆盖关系补到新页签权限点（必须在第 4 步退役之前执行）
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
  FROM sys_role_menu rm
  JOIN sys_menu src ON src.menu_id = rm.menu_id AND src.perms = 'report:interpretation:query'
  JOIN sys_menu m ON m.perms IN ('report:interpretation:lims',
                                 'report:interpretation:files',
                                 'report:interpretation:variants');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
  FROM sys_role_menu rm
  JOIN sys_menu src ON src.menu_id = rm.menu_id AND src.perms = 'report:interpretation:edit'
  JOIN sys_menu m ON m.perms IN ('report:interpretation:variants',
                                 'report:interpretation:preview');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
  FROM sys_role_menu rm
  JOIN sys_menu src ON src.menu_id = rm.menu_id AND src.perms = 'report:interpretation:generate'
  JOIN sys_menu m ON m.perms = 'report:interpretation:preview';

-- 3.1 持有任一页签权限的角色补「进入解读」：页签只能从列表页「解读」按钮进来，缺这个权限点会挂在入口
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, m.menu_id
  FROM sys_role_menu rm
  JOIN sys_menu tab ON tab.menu_id = rm.menu_id
   AND tab.perms IN ('report:interpretation:lims',
                     'report:interpretation:files',
                     'report:interpretation:variants',
                     'report:interpretation:preview',
                     'report:interpretation:review',
                     'report:interpretation:send')
  JOIN sys_menu m ON m.perms = 'report:interpretation:enter';

-- 4. 退役旧权限点：先解绑角色授权，再删权限点本身
DELETE rm FROM sys_role_menu rm
  JOIN sys_menu m ON m.menu_id = rm.menu_id
 WHERE m.perms IN ('report:interpretation:query',
                   'report:interpretation:edit',
                   'report:interpretation:generate');

DELETE FROM sys_menu
 WHERE perms IN ('report:interpretation:query',
                 'report:interpretation:edit',
                 'report:interpretation:generate');

-- 5. 复核（人工核对用，取 @menu_id 需重跑一次变量赋值）
-- SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);
-- SET @menu_id = (SELECT menu_id FROM sys_menu
--     WHERE parent_id = @report_menu_id AND path = 'interpretation' AND menu_type = 'C' LIMIT 1);
-- SELECT m.menu_id, m.menu_name, m.perms, m.order_num FROM sys_menu m
--  WHERE m.menu_id = @menu_id OR m.parent_id = @menu_id ORDER BY m.order_num;
-- SELECT r.role_id, r.role_name, m.perms FROM sys_role_menu rm
--   JOIN sys_role r ON r.role_id = rm.role_id JOIN sys_menu m ON m.menu_id = rm.menu_id
--  WHERE m.perms LIKE 'report:interpretation%' ORDER BY r.role_id, m.order_num;
