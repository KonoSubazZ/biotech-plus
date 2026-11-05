-- 菜单调整（2026-10-21）：样本信息 从「报告管理」移到「样本管理」（原「质控管理」）目录下，order=1；
-- 同时把一级目录文案「质控管理」改成「样本管理」。
-- 幂等：全部按 path 定位、不写死 menu_id，重复执行结果一致；只改层级/排序/文案，
-- 不动 menu_id → sys_role_menu 授权与按钮权限点位（report:sampleInfo:*）完全不受影响。
-- 回滚：把 parent_id 改回 report 目录、order 改回 1，并把 menu_name 改回「质控管理」。
SET NAMES utf8mb4;

-- 1. 一级目录改文案（path='qc' 仍保留，前端 URL /qc/sample-info 靠它，不建议改 path）
UPDATE sys_menu
SET menu_name = '样本管理', remark = '样本管理（一级目录）', update_by = 1, update_time = NOW()
WHERE parent_id = 0 AND path = 'qc' AND menu_type = 'M';

-- 2. 样本信息 挂到样本管理目录下，排第 1
SET @qc_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'qc' LIMIT 1);
SET @sample_id = (SELECT menu_id FROM sys_menu
    WHERE path = 'sample-info' AND menu_type = 'C' AND perms = 'report:sampleInfo:list'
    ORDER BY menu_id LIMIT 1);

UPDATE sys_menu
SET parent_id = @qc_id, order_num = 1, update_by = 1, update_time = NOW()
WHERE menu_id = @sample_id;

-- 3. 原有两个页面顺延，避免 order 撞车（湿实验质控 2 / 生信质控 3）
UPDATE sys_menu
SET order_num = 2, update_by = 1, update_time = NOW()
WHERE parent_id = @qc_id AND path = 'wet-lab' AND menu_type = 'C';

UPDATE sys_menu
SET order_num = 3, update_by = 1, update_time = NOW()
WHERE parent_id = @qc_id AND path = 'bioinfo' AND menu_type = 'C';

-- 4. 自检输出（执行后应看到：样本管理 | sample-info / wet-lab / bioinfo，order 1/2/3）
SELECT m.menu_id, m.menu_name, m.path, m.order_num, m.menu_type, m.perms
FROM sys_menu m
WHERE m.menu_id IN (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'qc')
   OR m.parent_id = @qc_id
ORDER BY m.parent_id, m.order_num;
