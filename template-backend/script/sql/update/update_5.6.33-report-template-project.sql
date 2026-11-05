-- 模板配置（report_template）增加项目维度 + 「模板配置」菜单移到项目管理下
--
-- 背景（2026-10-22 用户口径）：
--   1) 模板必须先选项目、再选产品，「项目 + 产品」加起来才是唯一：
--      同一项目下同一产品只允许绑定一个模板（保存时校验，冲突直接报错）；模板本身仍可挂多个产品。
--   2) 菜单顺序：项目管理 > 1 项目配置 / 2 产品配置 / 3 质控标准 / 4 模板配置（模板配置从报告管理移过来）。
--
-- 幂等：ADD COLUMN 用「列不存在才 ALTER」；索引/菜单用「存在才跳过」；全程可连跑两次。

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 1. 加列：project_id（逻辑关联 project_config.id，跨模块只读，不建外键）
--    存量行先留 NULL（老模板没有项目归属），新建/编辑时由 BO 的 @NotNull 强制必填。
-- ---------------------------------------------------------------------------
SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `report_template` ADD COLUMN `project_id` bigint NULL COMMENT ''逻辑关联project_config.id（模板归属项目）'' AFTER `customer_code`',
        'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_template' AND COLUMN_NAME = 'project_id');
PREPARE stmt_project_id FROM @ddl;
EXECUTE stmt_project_id;
DEALLOCATE PREPARE stmt_project_id;

SET @ddl = (SELECT IF(COUNT(*) = 0,
        'ALTER TABLE `report_template` ADD INDEX `idx_report_template_project` (`project_id`, `status`, `del_flag`)',
        'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_template' AND INDEX_NAME = 'idx_report_template_project');
PREPARE stmt_project_idx FROM @ddl;
EXECUTE stmt_project_idx;
DEALLOCATE PREPARE stmt_project_idx;

-- ---------------------------------------------------------------------------
-- 2. 存量回填：模板名里带了项目编号的（如 SOP-...-NG0101-T001...）按编号落到对应项目。
--    匹配不到就留 NULL —— 不编造归属，页面上一眼能看出来哪些还没分项目。
-- ---------------------------------------------------------------------------
UPDATE report_template t
JOIN project_config p ON p.del_flag = '0' AND t.template_name LIKE CONCAT('%', p.project_no, '%')
SET t.project_id = p.id
WHERE t.project_id IS NULL AND t.del_flag = '0';

-- ---------------------------------------------------------------------------
-- 3. 菜单迁移：「模板配置」从报告管理（path='report'）挂到项目管理（path='project'）下
--    menu_id 不变 → 已授权的角色仍然有效；只改父级、路径、组件与排位。
-- ---------------------------------------------------------------------------
SET @project_menu_id = (SELECT menu_id FROM sys_menu WHERE path = 'project' AND parent_id = 0 LIMIT 1);
SET @template_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = (SELECT menu_id FROM sys_menu WHERE path = 'report' AND parent_id = 0 LIMIT 1)
      AND path = 'template' LIMIT 1);
-- 兜底：已经被迁过一次（path 已改成 template-config）时也要能定位到
SET @template_menu_id = IFNULL(@template_menu_id, (SELECT menu_id FROM sys_menu
    WHERE parent_id = @project_menu_id AND path = 'template-config' LIMIT 1));

UPDATE sys_menu
SET parent_id = @project_menu_id,
    order_num = 4,
    path      = 'template-config',
    component = 'project/template-config/index',
    menu_name = 'route.project_template-config'
WHERE menu_id = @template_menu_id;

-- 项目管理下的排位：1 项目配置 / 2 产品配置 / 3 质控标准 / 4 模板配置
UPDATE sys_menu SET order_num = 1 WHERE parent_id = @project_menu_id AND path = 'project-config';
UPDATE sys_menu SET order_num = 2 WHERE parent_id = @project_menu_id AND path = 'product-config';
UPDATE sys_menu SET order_num = 3 WHERE parent_id = @project_menu_id AND path = 'qc-standard';
UPDATE sys_menu SET order_num = 4 WHERE parent_id = @project_menu_id AND path = 'template-config';

-- ---------------------------------------------------------------------------
-- 4. 只授了「模板配置」子菜单、没授「项目管理」父菜单的角色要补授父菜单
--    （实测坑：父菜单未授权时 getRouters 不下发该分支，前端点菜单直接跳 /403）。
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT rm.role_id, @project_menu_id
FROM sys_role_menu rm
WHERE rm.menu_id = @template_menu_id
  AND @project_menu_id IS NOT NULL;
