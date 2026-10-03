-- 样本信息表（sample_file）+「报告管理」菜单与权限点位
-- 依据：docs/context/设计文档/spec.md §1.4（sample_file 字段设计）
--       原文是 PG/Prisma 写法（uuid 主键、varchar 存日期等），类型按 ai-rules/04-db-schema.md §7 映射：
--       uuid→bigint 自增、timestamp→datetime；**日期类字段按原文保持 varchar(20)** ——
--       上游 LIMS 回传的就是 "2026-09-17" 这种字符串，改成 date 会在导入时因脏值整体失败。
-- 规范：ai-rules/04-db-schema.md     模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）
-- 定位：挂在「报告管理」(path=report) 下；表前缀 sample_ 与 /report 对应（04-db-schema §1）

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 建表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sample_file` (
    -- 主键：单列 bigint 自增（全局 idType=AUTO，主键列必须 AUTO_INCREMENT）
    id                 bigint       NOT NULL AUTO_INCREMENT        COMMENT '主键',

    -- 业务字段（设计文档 spec.md §1.4）
    subbarcode         varchar(80)  NOT NULL                       COMMENT '样本编号（subbarcode，qc_record.subbarcode 关联它；也是导入时的去重键）',
    barcode            varchar(80)                                 COMMENT '条码',
    patient_id         varchar(80)                                 COMMENT '患者编号',
    person_name        varchar(80)                                 COMMENT '患者姓名',
    gender             varchar(10)                                 COMMENT '性别',
    birthday           varchar(20)                                 COMMENT '出生日期',
    age                varchar(10)                                 COMMENT '年龄',
    patient_phone      varchar(40)                                 COMMENT '患者电话',
    hospital           varchar(120)                                COMMENT '医院',
    received_date      varchar(20)                                 COMMENT '接收日期',
    specimen_type      varchar(60)                                 COMMENT '样本类型',
    specimen_quantity  varchar(60)                                 COMMENT '样本数量',
    testing_program    varchar(120)                                COMMENT '检测方案',
    disease_type       varchar(120)                                COMMENT '疾病类型（录单癌种）',
    client             varchar(120)                                COMMENT '客户',
    commission_date    varchar(20)                                 COMMENT '委托日期',
    product_name       varchar(120)                                COMMENT '产品名称（录单产品，关联 product_config.name）',
    remark             varchar(500)                                COMMENT '备注',

    -- 公共字段（04-db-schema §2；无 create_dept）
    create_by          bigint                                      COMMENT '创建者',
    create_time        datetime                                    COMMENT '创建时间',
    update_by          bigint                                      COMMENT '更新者',
    update_time        datetime                                    COMMENT '更新时间',
    del_flag           char(1)      NOT NULL DEFAULT '0'           COMMENT '删除标志（0代表存在 1代表删除）',

    -- 租户列：所有业务表必备
    tenant_id          varchar(20)  NOT NULL DEFAULT '000000'      COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 样本编号在一个租户内唯一：导入按它做「有则更新、无则新增」，
    -- 唯一键要求 tenant_id 打头（见 04-db-schema §4）
    UNIQUE KEY uk_sample_file_subbarcode (tenant_id, subbarcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='样本信息';


-- ============================================================================
-- 2. 菜单：报告管理（一级目录）> 样本信息
--    幂等：每段都用 NOT EXISTS 包一层派生表（MySQL 5.7 不支持相关派生表，用 @变量传值）
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告管理', 0, 3, 'report', NULL, 1, 0,
    'M', '0', '0', '', 'local-icon-post', 1, NOW(), '报告管理（目录）'
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = 0 AND path = 'report') AS exists_check);

SET @report_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.report_sample-info', @report_id, 1, 'sample-info', 'report/sample-info/index', 1, 0,
    'C', '0', '0', 'report:sampleInfo:list', 'local-icon-clipboard', 1, NOW(), '样本信息'
FROM (SELECT 1) AS dummy
WHERE @report_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @report_id AND path = 'sample-info') AS exists_check);

SET @menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @report_id AND path = 'sample-info' LIMIT 1);


-- ============================================================================
-- 按钮权限（F 类型）—— 与 @SaCheckPermission 逐字一致
-- 导入单列一个点位（report:sampleInfo:import）：导入会覆盖既有样本，允许单独授权/收权
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:query') AS c1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:add') AS c2);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:edit') AS c3);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', @menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:remove', '#', 1, NOW(), '删除'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:remove') AS c4);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '导入', @menu_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:import', '#', 1, NOW(), '导入'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:import') AS c5);


-- ============================================================================
-- 4. 授权给超级管理员角色（sys_role_menu 主键 (role_id, menu_id)，INSERT IGNORE 保证幂等）
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @report_id OR m.menu_id = @menu_id OR m.parent_id = @menu_id;
