-- 产品配置表（product_config）
-- 依据：docs/context/设计文档/product-config.md §1（数据模型）+ spec.md §1.3
--      原文是 PG/Prisma 写法（uuid 主键、_created_at 等），类型已按规范映射
-- 规范：ai-rules/04-db-schema.md     模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）
-- 定位：挂在「项目管理」菜单下（设计文档原写 /system/product-config，本次按项目管理的归属实现）

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `product_config` (
    id                bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',

    -- 业务字段（2026-11-03 对齐产品配置图，见 update_5.6.51-product-config-refactor.sql）
    --   product_bio  = 生信侧产品名，逐字等于上传目录第 2 层 / analysis_data.product_bio（驱动与报告定位都靠它）
    --   product_name = 系统用产品名（页面、报告模板下拉）
    --   检测方式不入库（业务默认「来样即检」）；panel_type 图里没有，冗余保留
    product_code        varchar(60)  NOT NULL                          COMMENT '产品编码',
    trade_name          varchar(120)                                   COMMENT '商品名',
    product_bio         varchar(120) NOT NULL                          COMMENT '生信编码（= 上传目录第 2 层 product，与 analysis_data.product_bio 逐字相等）',
    product_name        varchar(120) NOT NULL                          COMMENT '产品名称（系统用）',
    related_diseases    varchar(500)                                   COMMENT '适应症',
    test_type           varchar(60)                                    COMMENT '样本类型（组织 / 血液）',
    control_type        varchar(60)                                    COMMENT '对照（血液 / PBMC / 无）',
    product_version     varchar(30)                                    COMMENT '版本（LDT / IVD）',
    sequencing_platform varchar(60)                                    COMMENT '测序平台（surf5000 Dx 等）',
    gene_count          int                                            COMMENT '基因数/DNA（panel 基因个数，如 1238）',
    report_cycle_days   int                                            COMMENT '报告周期（天）',
    maintain_lab        varchar(60)                                    COMMENT '维护实验室',
    related_process     varchar(100)                                   COMMENT '关联工序（如 全流程）',
    probe_type          varchar(120)                                   COMMENT '探针类型（如 正常1238探针 / 正常1238探针+UMI）',
    panel_type          varchar(60)                                    COMMENT 'panel类型（DNA / RNA / DNA_RNA / WES / HRD 等，图里没有，冗余保留）',
    status              varchar(20)  NOT NULL DEFAULT 'active'         COMMENT '状态（active 启用 / inactive 停用）',
    remark              varchar(500)                                   COMMENT '备注',

    -- 公共字段（04-db-schema §2；无 create_dept）
    create_by         bigint                                         COMMENT '创建者',
    create_time       datetime                                       COMMENT '创建时间',
    update_by         bigint                                         COMMENT '更新者',
    update_time       datetime                                       COMMENT '更新时间',
    del_flag          char(1)      NOT NULL DEFAULT '0'              COMMENT '删除标志（0代表存在 1代表删除）',

    -- 租户列：所有业务表必备
    tenant_id         varchar(20)  NOT NULL DEFAULT '000000'         COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 驱动/报告按生信编码反查产品
    KEY idx_product_bio (tenant_id, product_bio),
    -- 规范要求 tenant_id 打头（不同租户可以有相同编码的产品）
    UNIQUE KEY uk_product_config_product_code (tenant_id, product_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='产品配置';


-- ============================================================================
-- 菜单：挂到「项目管理」下（用 path 定位父菜单，不写死 ID）
--   perms 与 ProductConfigController 的 @SaCheckPermission 逐字一致
-- ============================================================================
SET @parent_id = (SELECT menu_id FROM sys_menu WHERE path = 'project' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.project_product-config', @parent_id, 2, 'product-config', 'project/product-config/index', 1, 0,
    'C', '0', '0', 'project:productConfig:list', 'local-icon-company', 1, NOW(), '产品配置'
FROM (SELECT 1) AS dummy
WHERE @parent_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @parent_id AND path = 'product-config') AS exists_check);

SET @menu_id = (SELECT menu_id FROM sys_menu WHERE path = 'product-config' AND parent_id = @parent_id LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'project:productConfig:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:productConfig:query') AS c1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'project:productConfig:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:productConfig:add') AS c2);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'project:productConfig:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:productConfig:edit') AS c3);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', @menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'project:productConfig:remove', '#', 1, NOW(), '删除'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'project:productConfig:remove') AS c4);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @menu_id OR m.parent_id = @menu_id;
