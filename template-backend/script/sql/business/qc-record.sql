-- 质控记录表（qc_record）+「质控管理」菜单（湿实验质控 / 生信质控）与权限点位
-- 依据：docs/context/设计文档/spec.md §1.1（qc_record 字段表）
--       docs/context/设计文档/bioinfo-qc.md（生信质控 = 同表同接口，仅 qc_category='bioinfo'）
--       原文是 PG/Prisma 写法（uuid 主键、_created_at 等），类型已按 ai-rules/04-db-schema.md §7 映射
-- 规范：ai-rules/04-db-schema.md     模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）
-- 菜单位置：「质控管理」是根目录下的一级菜单（2026-10 从「项目管理 > 质量管理」独立出来），
--       前端页面 /qc/wet-lab、/qc/bioinfo（与设计文档原写的 /qc/quality-control 同一层）

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 建表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `qc_record` (
    -- 主键：单列 bigint 自增（全局 idType=AUTO，主键列必须 AUTO_INCREMENT）
    id            bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',

    -- 业务字段（设计文档 spec.md §1.1）
    subbarcode    varchar(80)  NOT NULL                          COMMENT '样本条码（关联 sample_file.subbarcode）',
    qc_item       varchar(100) NOT NULL                          COMMENT '质控项目名称（如 mapping_rate、average_depth）',
    qc_result     varchar(50)                                    COMMENT '质控结果数值（字符串形式，如 98.5；非数值在判定时归 non_numeric）',
    `operator`    varchar(80)                                    COMMENT '操作员',
    tested_at     datetime                                       COMMENT '检测时间',
    remark        varchar(500)                                   COMMENT '备注',
    status        varchar(20)  NOT NULL DEFAULT 'pending'        COMMENT '人工确认状态（pending 待确认 / passed 通过 / failed 未通过）',
    qc_category   varchar(20)  NOT NULL DEFAULT 'wet_lab'        COMMENT '质控类别（wet_lab 湿实验 / bioinfo 生信）',

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
    -- 设计文档「idx_qc_record_subbarcode(subbarcode)」；规范要求 tenant_id 打头（租户过滤总是第一个条件）
    KEY idx_qc_record_subbarcode (tenant_id, subbarcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='质控记录';


-- ============================================================================
-- 2. 菜单：质控管理（根目录下的一级菜单）> 湿实验质控 / 生信质控
--    一级目录：parent_id=0、path='qc'、component='Layout'；
--    两个页面 component 写满三层（qc/wet-lab/index，路由名 qc_wet-lab / qc_bioinfo），
--    与前端目录 views/qc/{wet-lab,bioinfo}/ 一一对应（目录层级必须与菜单层级一致）。
--    用 path 定位菜单，不写死 ID；INSERT 用 NOT EXISTS 包一层派生表做幂等（MySQL 5.7 不能相关派生表）；
--    历史数据（原先挂在「项目管理 > 质量管理」下）用 UPDATE 收敛过来，脚本可重复执行。
--    perms 与 QcRecordController 的 @SaCheckPermission 逐字一致
-- ============================================================================
SET @project_id = (SELECT menu_id FROM sys_menu WHERE path = 'project' AND parent_id = 0 LIMIT 1);

-- 旧结构收敛：把「项目管理 > 质量管理」那行原地搬成根目录一级菜单（保留 menu_id，角色授权不用重做）
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

-- 湿实验质控（component 三层：qc/wet-lab/index，路由名 qc_wet-lab）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.qc_wet-lab', @quality_id, 1, 'wet-lab', 'qc/wet-lab/index', 1, 0,
    'C', '0', '0', 'qc:qcRecord:list', 'local-icon-documentation', 1, NOW(), '湿实验质控'
FROM (SELECT 1) AS dummy
WHERE @quality_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @quality_id AND path = 'wet-lab') AS exists_check);

UPDATE sys_menu
SET parent_id = @quality_id, menu_name = 'route.qc_wet-lab', component = 'qc/wet-lab/index',
    path = 'wet-lab', order_num = 1, menu_type = 'C', update_by = 1, update_time = NOW()
WHERE parent_id = @quality_id AND path = 'wet-lab';

-- 生信质控（component 三层：qc/bioinfo/index，路由名 qc_bioinfo）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.qc_bioinfo', @quality_id, 2, 'bioinfo', 'qc/bioinfo/index', 1, 0,
    'C', '0', '0', 'qc:qcRecord:list', 'local-icon-code', 1, NOW(), '生信质控'
FROM (SELECT 1) AS dummy
WHERE @quality_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @quality_id AND path = 'bioinfo') AS exists_check);

UPDATE sys_menu
SET parent_id = @quality_id, menu_name = 'route.qc_bioinfo', component = 'qc/bioinfo/index',
    path = 'bioinfo', order_num = 2, menu_type = 'C', update_by = 1, update_time = NOW()
WHERE parent_id = @quality_id AND path = 'bioinfo';


-- ============================================================================
-- 3. 按钮权限（F 类型）—— 与后端 @SaCheckPermission 一一对应
--    两个页面共用同一个实体/接口，故两棵菜单各挂一套（角色只授其一也能出按钮）
-- ============================================================================
SET @wet_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @quality_id AND path = 'wet-lab' LIMIT 1);
SET @bio_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @quality_id AND path = 'bioinfo' LIMIT 1);

-- 查询
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', m.menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'qc:qcRecord:query', '#', 1, NOW(), '查询'
FROM (SELECT @wet_id AS menu_id UNION ALL SELECT @bio_id) AS m
WHERE m.menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu x
    WHERE x.parent_id = m.menu_id AND x.perms = 'qc:qcRecord:query');

-- 新增
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', m.menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'qc:qcRecord:add', '#', 1, NOW(), '新增'
FROM (SELECT @wet_id AS menu_id UNION ALL SELECT @bio_id) AS m
WHERE m.menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu x
    WHERE x.parent_id = m.menu_id AND x.perms = 'qc:qcRecord:add');

-- 修改（页面上的「确认状态」也走这个权限）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', m.menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'qc:qcRecord:edit', '#', 1, NOW(), '修改'
FROM (SELECT @wet_id AS menu_id UNION ALL SELECT @bio_id) AS m
WHERE m.menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu x
    WHERE x.parent_id = m.menu_id AND x.perms = 'qc:qcRecord:edit');

-- 删除
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', m.menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'qc:qcRecord:remove', '#', 1, NOW(), '删除'
FROM (SELECT @wet_id AS menu_id UNION ALL SELECT @bio_id) AS m
WHERE m.menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu x
    WHERE x.parent_id = m.menu_id AND x.perms = 'qc:qcRecord:remove');


-- ============================================================================
-- 4. 授权给超级管理员角色（sys_role_menu 主键是 (role_id, menu_id)，用 INSERT IGNORE 保证幂等）
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (@quality_id, @wet_id, @bio_id)
   OR m.parent_id IN (@wet_id, @bio_id);
