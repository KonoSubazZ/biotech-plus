-- 项目配置表（project_config）
-- 依据：用户需求「项目管理 > 项目配置」——一个项目一行，维护报告发送用的收件/抄送邮箱
-- 规范：ai-rules/04-db-schema.md     模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）
-- 定位：挂在「项目管理」菜单下（与产品配置、质控标准同级）

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `project_config` (
    id            bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',

    -- 业务字段
    project_no    varchar(60)  NOT NULL                          COMMENT '项目编号（同一租户内唯一）',
    project_name  varchar(200) NOT NULL                          COMMENT '项目名称',
    customer      varchar(200) NOT NULL                          COMMENT '客户名称',
    receive_email varchar(500) NOT NULL                          COMMENT '收件邮箱（多个用分号分隔）',
    cc_email      varchar(500)                                   COMMENT '抄送邮箱（多个用分号分隔）',
    remark        varchar(500)                                   COMMENT '备注',

    -- 公共字段（04-db-schema §2；无 create_dept）
    create_by     bigint                                         COMMENT '创建者',
    create_time   datetime                                       COMMENT '创建时间',
    update_by     bigint                                         COMMENT '更新者',
    update_time   datetime                                       COMMENT '更新时间',
    del_flag      char(1)      NOT NULL DEFAULT '0'              COMMENT '删除标志（0代表存在 1代表删除）',

    -- 租户列：所有业务表必备
    tenant_id     varchar(20)  NOT NULL DEFAULT '000000'         COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 一个项目只有一条配置：唯一键 tenant_id 打头（不同租户可以有相同项目编号）
    UNIQUE KEY uk_project_config_project_no (tenant_id, project_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目配置';


-- ============================================================================
-- 菜单：挂到「项目管理」下（用 path 定位父菜单，不写死 ID）
--   perms 与 ProjectConfigController 的 @SaCheckPermission 逐字一致
-- ============================================================================
SET @parent_id = (SELECT menu_id FROM sys_menu WHERE path = 'project' AND parent_id = 0 LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.project_project-config', @parent_id, 1, 'project-config', 'project/project-config/index', 1, 0,
    'C', '0', '0', 'project:projectConfig:list', 'local-icon-company', 1, NOW(), '项目配置'
FROM (SELECT 1) AS dummy
WHERE @parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @parent_id AND path = 'project-config') AS exists_check);

SET @menu_id = (SELECT menu_id FROM sys_menu WHERE path = 'project-config' AND parent_id = @parent_id LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'project:projectConfig:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:projectConfig:query') AS c1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'project:projectConfig:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:projectConfig:add') AS c2);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'project:projectConfig:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:projectConfig:edit') AS c3);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', @menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'project:projectConfig:remove', '#', 1, NOW(), '删除'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:projectConfig:remove') AS c4);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @menu_id OR m.parent_id = @menu_id;
