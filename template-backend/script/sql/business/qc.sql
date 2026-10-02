-- 质控标准表（qc_standard）+ 菜单与权限点位
-- 依据：docs/context/设计文档/spec.md §1.2（字段表；原文是 PG/Prisma 写法，类型已按规范映射）
-- 规范：ai-rules/04-db-schema.md（主键自增 / 公共字段 / 租户列 / 索引 / 排序规则）
-- 模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 建表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `qc_standard` (
    -- 主键：单列 bigint 自增（全局 idType=AUTO，主键列必须 AUTO_INCREMENT）
    id            bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',

    -- 业务字段（设计文档 §1.2）
    product_id    bigint       NOT NULL                          COMMENT '关联产品（product_config.id）',
    qc_item       varchar(100) NOT NULL                          COMMENT '质控项目名称（与 qc_record.qc_item 对应）',
    qc_category   varchar(20)  NOT NULL DEFAULT 'wet_lab'        COMMENT '质控类别（wet_lab 湿实验 / bioinfo 生信）',
    min_value     varchar(50)                                    COMMENT '合格下限（NULL 表示不设下限）',
    max_value     varchar(50)                                    COMMENT '合格上限（NULL 表示不设上限）',
    unit          varchar(30)                                    COMMENT '单位（如 %、X）',
    status        varchar(20)  NOT NULL DEFAULT 'active'         COMMENT '状态（active 启用 / inactive 停用）',
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
    -- 设计文档「唯一索引 (product_id, qc_item, qc_category)」+ 规范要求 tenant_id 打头。
    -- 该索引同时覆盖「按租户+产品」的查询，故不另建 idx_product_id（避免冗余索引）。
    UNIQUE KEY uk_qc_standard_product_item_category (tenant_id, product_id, qc_item, qc_category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='质控标准';


-- ============================================================================
-- 2. 菜单（挂到「项目管理」下，用 path 定位父菜单，不写死 ID）
--    perms 与 QcStandardController 的 @SaCheckPermission 逐字一致
--    幂等：每段都用 NOT EXISTS 包一层派生表（不引用外层别名，避免 MySQL 的相关子查询限制）
-- ============================================================================
SET @parent_id = (SELECT menu_id FROM sys_menu WHERE path = 'project' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.project_qc-standard', @parent_id, 1, 'qc-standard', 'project/qc-standard/index', 1, 0,
    'C', '0', '0', 'qc:qcStandard:list', 'local-icon-dict', 1, NOW(), '质控标准'
FROM (SELECT 1) AS dummy
WHERE @parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @parent_id AND path = 'qc-standard') AS exists_check);

SET @menu_id = (SELECT menu_id FROM sys_menu WHERE path = 'qc-standard' AND parent_id = @parent_id LIMIT 1);


-- ============================================================================
-- 3. 按钮权限（F 类型）—— 与后端 @SaCheckPermission 一一对应
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'qc:qcStandard:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'qc:qcStandard:query') AS c1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'qc:qcStandard:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'qc:qcStandard:add') AS c2);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'qc:qcStandard:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'qc:qcStandard:edit') AS c3);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', @menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'qc:qcStandard:remove', '#', 1, NOW(), '删除'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'qc:qcStandard:remove') AS c4);


-- ============================================================================
-- 4. 授权给超级管理员角色（sys_role_menu 主键是 (role_id, menu_id)，用 INSERT IGNORE 保证幂等）
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @menu_id OR m.parent_id = @menu_id;
