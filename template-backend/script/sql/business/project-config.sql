-- 项目配置表（project_config）
-- 依据：用户需求「项目管理 > 项目配置」——一个项目一行，维护报告发送用的收件/抄送邮箱
-- 2026-10-28 重构：项目编号下可挂多个研究中心，每个中心各自维护收件/抄送邮箱。
--   单表自关联：parent_id=0 是项目行，parent_id=项目行 id 是研究中心行；
--   不建中心主表/关联表。唯一键 uk_project_config_node(tenant_id, project_no,
--   research_center_no, research_center_name) 同时管住「一个项目编号一行」与「同一项目下（编号,名称）不重复」。
--   contract_name / contract_code / study_plan_no 是项目级展示字段，不参与业务逻辑；
--   research_center_no 与 research_center_name 成对，中心行必填、项目行空串。
-- 规范：ai-rules/04-db-schema.md     模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）
-- 定位：挂在「项目管理」菜单下（与产品配置、质控标准同级）

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `project_config` (
    id            bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',
    parent_id     bigint       NOT NULL DEFAULT 0                COMMENT '父节点ID（0=项目行；研究中心行=所属项目行 id）',

    -- 业务字段
    project_no    varchar(60)  NOT NULL                          COMMENT '项目编号（项目行唯一；中心行冗余存）',
    project_name  varchar(200) NOT NULL DEFAULT ''               COMMENT '项目名称（仅项目行有值）',
    customer      varchar(200) NOT NULL DEFAULT ''               COMMENT '客户名称（仅项目行有值）',
    contract_name varchar(200)           DEFAULT NULL            COMMENT '合同名称（仅项目行有值；展示字段，不参与业务逻辑）',
    contract_code varchar(100)           DEFAULT NULL            COMMENT '合同编号（仅项目行有值；展示字段）',
    study_plan_no varchar(100)           DEFAULT NULL            COMMENT '研究方案编号（仅项目行有值；展示字段）',
    research_center_no   varchar(100) NOT NULL DEFAULT ''        COMMENT '研究中心编号（仅中心行有值；项目行为空串，与名称成对参与唯一键）',
    research_center_name varchar(200) NOT NULL DEFAULT ''        COMMENT '研究中心名称（仅中心行有值；项目行为空串，参与唯一键）',
    receive_email varchar(500) NOT NULL DEFAULT ''               COMMENT '收件邮箱（;分隔）；项目行=项目自身，中心行=该中心',
    cc_email      varchar(500)                                   COMMENT '抄送邮箱（;分隔）；项目行=项目自身，中心行=该中心',
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
    KEY idx_project_config_parent (tenant_id, parent_id),
    -- 项目行两列空串 → 一个项目编号一行；中心行 → 同项目内（编号,名称）不重复
    UNIQUE KEY uk_project_config_node (tenant_id, project_no, research_center_no, research_center_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='项目配置（项目行 + 研究中心行，parent_id 自关联）';


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
