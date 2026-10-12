-- 「质控管理」菜单（一级目录 + 湿实验质控 / 生信质控 两个页面 + 按钮权限点位）
-- 来历：本文件原为 qc-record.sql（含 qc_record 建表与菜单）。2026-10-12 起：
--   · qc_record 表与它的 CRUD 模块整体下线，湿实验质控改走 wet_lab_qc（见 business/wet-lab-qc.sql，
--     接口灌数据的那套：POST /qc/wetLabQc/receive）；
--   · 本文件只负责「质控管理」菜单层级与权限点位，perms 与各 Controller 的 @SaCheckPermission 逐字一致。
-- 菜单位置：「质控管理」是根目录下的一级菜单，前端页面 /qc/wet-lab、/qc/bioinfo
--   （与设计文档原写的 /qc/quality-control 同一层，路径以本文件为准）。
-- 幂等：按 path/perms 定位 + NOT EXISTS 包一层派生表（MySQL 5.7 不能相关派生表），脚本可重复执行。
-- 注意：本文件按文件名排在 wet-lab-qc.sql、bioinfo-qc.sql 之前，所以这里用**最终**的权限点建行；
--   那两个文件里的 UPDATE（旧值 → 新值）只对已有库生效，新装库上自然匹配不到、无副作用。
-- 体检：python3 tools/check_db_schema.py

SET NAMES utf8mb4;

-- ============================================================================
-- 0. 老 qc_record 表下线（新装库本来就没有这张表，这行是给「重跑业务脚本的库」兜底）
--    表里若还有历史数据，先自己 dump 备份再执行；已有库的迁移走
--    script/sql/update/update_5.6.13-drop-qc-record.sql（同一个动作 + 清菜单遗留行）
-- ============================================================================
DROP TABLE IF EXISTS `qc_record`;


-- ============================================================================
-- 1. 一级目录：质控管理（parent_id=0、path='qc'、component='Layout'）
--    历史数据（原先挂在「项目管理 > 质量管理」下）用 UPDATE 收敛过来，保留 menu_id → 角色授权不用重做
-- ============================================================================
SET @project_id = (SELECT menu_id FROM sys_menu WHERE path = 'project' AND parent_id = 0 LIMIT 1);

SET @legacy_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @project_id AND path = 'quality' LIMIT 1);

UPDATE sys_menu
SET parent_id = 0, menu_name = '质控管理', path = 'qc', component = 'Layout', order_num = 3,
    menu_type = 'M', visible = '0', status = '0', remark = '质控管理（一级目录）',
    update_by = 1, update_time = NOW()
WHERE menu_id = @legacy_id;

-- 新装：没有历史行时新建（已迁过的库这里自然跳过）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '质控管理', 0, 3, 'qc', 'Layout', 1, 0,
    'M', '0', '0', '', 'local-icon-skill', 1, NOW(), '质控管理（一级目录）'
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = 0 AND path = 'qc') AS exists_check);

SET @quality_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'qc' LIMIT 1);

-- 收敛到当前定义：无论这行是刚搬过来的旧行还是已迁过的库，这里都把字段刷成同一份定义
-- （旧行的 UPDATE 只认「旧位置」，第二次执行就查不到了，所以必须按当前位置再刷一次）
UPDATE sys_menu
SET parent_id = 0, menu_name = '质控管理', path = 'qc', component = 'Layout', order_num = 3,
    menu_type = 'M', visible = '0', status = '0', remark = '质控管理（一级目录）',
    update_by = 1, update_time = NOW()
WHERE menu_id = @quality_id;


-- ============================================================================
-- 2. 两个页面菜单：湿实验质控（qc:wetLabQc:list）/ 生信质控（qc:bioinfo:list）
--    component 写满三层（qc/wet-lab/index），与前端目录 views/qc/{wet-lab,bioinfo}/ 一一对应
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.qc_wet-lab', @quality_id, 1, 'wet-lab', 'qc/wet-lab/index', 1, 0,
    'C', '0', '0', 'qc:wetLabQc:list', 'local-icon-documentation', 1, NOW(), '湿实验质控'
FROM (SELECT 1) AS dummy
WHERE @quality_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @quality_id AND path = 'wet-lab') AS exists_check);

UPDATE sys_menu
SET parent_id = @quality_id, menu_name = 'route.qc_wet-lab', component = 'qc/wet-lab/index',
    path = 'wet-lab', order_num = 1, menu_type = 'C', perms = 'qc:wetLabQc:list',
    visible = '0', status = '0', update_by = 1, update_time = NOW()
WHERE parent_id = @quality_id AND path = 'wet-lab';

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.qc_bioinfo', @quality_id, 2, 'bioinfo', 'qc/bioinfo/index', 1, 0,
    'C', '0', '0', 'qc:bioinfo:list', 'local-icon-code', 1, NOW(), '生信质控'
FROM (SELECT 1) AS dummy
WHERE @quality_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @quality_id AND path = 'bioinfo') AS exists_check);

UPDATE sys_menu
SET parent_id = @quality_id, menu_name = 'route.qc_bioinfo', component = 'qc/bioinfo/index',
    path = 'bioinfo', order_num = 2, menu_type = 'C', perms = 'qc:bioinfo:list',
    visible = '0', status = '0', update_by = 1, update_time = NOW()
WHERE parent_id = @quality_id AND path = 'bioinfo';

SET @wet_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @quality_id AND path = 'wet-lab' LIMIT 1);
SET @bio_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @quality_id AND path = 'bioinfo' LIMIT 1);


-- ============================================================================
-- 3. 按钮权限点（F 类型）—— 与各 Controller 的 @SaCheckPermission 逐字一致
--    湿实验质控：查询 / 接口接收 / 修改 / 删除（没有新增：数据只由接口灌）
--    生信质控：查询 / 修改 / 删除 / 接口接收 + 质控不合格通知的查询 / 生成
--    （各模块自己的 business 文件也为「已有库」补同样的行；两边都带 NOT EXISTS，重复定义不会重复插）
--    🔴 判重写法只能用「子查询直接查 sys_menu 并关联外层别名」：
--       NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = m.menu_id AND x.perms = m.perms)
--       写成 NOT EXISTS (SELECT 1 FROM (SELECT ... FROM sys_menu) AS existing WHERE existing.menu_id = m.menu_id)
--       在 MySQL 5.7 上**关联不上**（比较恒为 NULL）→ NOT EXISTS 恒真 → 每跑一遍插一份重复行（实测踩过）。
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT m.menu_name, m.menu_id, m.order_num, '', NULL, 1, 0, 'F', '0', '0', m.perms, '#', 1, NOW(), m.remark
FROM (SELECT @wet_menu_id AS menu_id, '查询' AS menu_name, 1 AS order_num, 'qc:wetLabQc:query' AS perms, '查询' AS remark
      UNION ALL SELECT @wet_menu_id, '接口接收', 2, 'qc:wetLabQc:receive', '接口推送'
      UNION ALL SELECT @wet_menu_id, '修改', 3, 'qc:wetLabQc:edit', '人工修正质控值'
      UNION ALL SELECT @wet_menu_id, '删除', 4, 'qc:wetLabQc:remove', '删除') AS m
WHERE m.menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu x
    WHERE x.parent_id = m.menu_id AND x.perms = m.perms);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT m.menu_name, m.menu_id, m.order_num, '', NULL, 1, 0, 'F', '0', '0', m.perms, '#', 1, NOW(), m.remark
FROM (SELECT @bio_menu_id AS menu_id, '查询' AS menu_name, 1 AS order_num, 'qc:bioinfo:query' AS perms, '查询' AS remark
      UNION ALL SELECT @bio_menu_id, '接口接收', 2, 'qc:bioinfo:receive', '接口接收'
      UNION ALL SELECT @bio_menu_id, '修改', 3, 'qc:bioinfo:edit', '修改'
      UNION ALL SELECT @bio_menu_id, '删除', 4, 'qc:bioinfo:remove', '删除'
      UNION ALL SELECT @bio_menu_id, '通知查询', 5, 'qc:notify:query', '质控不合格通知查询'
      UNION ALL SELECT @bio_menu_id, '通知生成', 6, 'qc:notify:generate', '质控不合格通知生成') AS m
WHERE m.menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu x
    WHERE x.parent_id = m.menu_id AND x.perms = m.perms);


-- ============================================================================
-- 4. 授权给超级管理员角色（sys_role_menu 主键是 (role_id, menu_id)，用 INSERT IGNORE 保证幂等）
--    覆盖三个菜单行 + 它们下面的按钮行（只授菜单不授按钮的话，超管以外的角色进页面看不到按钮）
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @quality_id
   OR m.parent_id = @quality_id
   OR m.parent_id IN (@wet_menu_id, @bio_menu_id);
