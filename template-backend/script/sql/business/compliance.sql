-- 「合规管理」菜单模块的建表与菜单/权限（一个菜单模块一个文件）
--   1) 开发记录表 dev_log        + 菜单/权限（2026-10 已落地）
--   2) 3Q 验证记录表 validation_record + 菜单/权限（2026-10 新增）
--   3) 3Q 文档管理表 compliance_document + 菜单/权限（2026-10 新增，记录类，**可删除**）
--
-- 依据：docs/context/设计文档/{dev-log,validation-record,compliance-docs}.md
--       原文是 PG/Prisma 写法（uuid 主键、_created_at/_updated_at/_updated_by），
--       类型已按 ai-rules/04-db-schema.md §7 映射到本仓（bigint 自增 + 公共字段四件套）。
-- 合规口径：开发记录 / 验证记录属「不可篡改的追溯证据」→ append-only，**不加 del_flag**、
--       不建「删除」按钮、后端不提供删除接口（ai-rules/04-db-schema.md §2「谁加 del_flag」）。
--       3Q 文档管理属**记录类**（文档库，可增删改）→ 带 del_flag + @TableLogic + 删除按钮。
-- 附件口径（三张表统一）：附件存服务器固定目录，库里**只记原文件名**，同名即替换，
--       下载按记录 id 定位；不存设计文档里的 file_url（避免路径注入与双份真相）。
-- 规范：ai-rules/04-db-schema.md      模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py（error 必须为 0）
-- 菜单位置：「合规管理」是根目录下的一级菜单

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 建表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `dev_log` (
    -- 主键：单列 bigint 自增（全局 idType=AUTO，主键列必须 AUTO_INCREMENT）
    id            bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',

    -- 业务字段（设计文档 dev-log.md §1；version / file_name 为 2026-10 页面新增，见 update_5.6.2-dev-log-version-file.sql）
    title         varchar(200) NOT NULL                          COMMENT '记录标题',
    category      varchar(20)  NOT NULL                          COMMENT '分类（feature 功能新增 / fix 缺陷修复 / change 变更调整）',
    version       varchar(50)                                    COMMENT '版本号',
    content       text                                           COMMENT '详细内容',
    file_name     varchar(255)                                   COMMENT '记录文档文件名（原文件名，附件存服务器固定目录）',
    developer     varchar(80)                                    COMMENT '开发人员',
    log_date      date                                           COMMENT '记录日期',

    -- 公共字段（04-db-schema §2；无 create_dept）
    create_by     bigint                                         COMMENT '创建者',
    create_time   datetime                                       COMMENT '创建时间',
    update_by     bigint                                         COMMENT '更新者',
    update_time   datetime                                       COMMENT '更新时间',

    -- 租户列：所有业务表必备
    tenant_id     varchar(20)  NOT NULL DEFAULT '000000'         COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 设计文档的 idx_dev_log_date(log_date) / idx_dev_log_category(category)；
    -- 规范要求 tenant_id 打头（租户过滤总是第一个条件）
    KEY idx_dev_log_date (tenant_id, log_date),
    KEY idx_dev_log_category (tenant_id, category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='开发记录';


-- ============================================================================
-- 2. 菜单：合规管理（一级目录）> 开发记录
--    一级目录：parent_id=0、path='compliance'、component='Layout'；
--    页面 component 写两层（compliance/dev-log/index，路由名 compliance_dev-log），
--    与前端目录 views/compliance/dev-log/ 一一对应（目录层级必须与菜单层级一致）。
--    用 path 定位菜单，不写死 ID；INSERT 用 NOT EXISTS 包一层派生表做幂等（MySQL 5.7 无 LATERAL）。
--    perms 与 DevLogController 的 @SaCheckPermission 逐字一致
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '合规管理', 0, 6, 'compliance', 'Layout', 1, 0,
    'M', '0', '0', '', 'local-icon-lock', 1, NOW(), '合规管理（一级目录）'
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = 0 AND path = 'compliance') AS exists_check);

SET @compliance_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'compliance' LIMIT 1);

-- 收敛到当前定义：无论这行是新装的还是已存在的库，字段都刷成同一份定义（脚本可重复执行）
UPDATE sys_menu
SET parent_id = 0, menu_name = '合规管理', path = 'compliance', component = 'Layout', order_num = 6,
    menu_type = 'M', visible = '0', status = '0', remark = '合规管理（一级目录）',
    update_by = 1, update_time = NOW()
WHERE menu_id = @compliance_id;

-- 开发记录（component 两层：compliance/dev-log/index，路由名 compliance_dev-log）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.compliance_dev-log', @compliance_id, 1, 'dev-log', 'compliance/dev-log/index', 1, 0,
    'C', '0', '0', 'compliance:devLog:list', 'local-icon-log', 1, NOW(), '开发记录'
FROM (SELECT 1) AS dummy
WHERE @compliance_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @compliance_id AND path = 'dev-log') AS exists_check);

UPDATE sys_menu
SET parent_id = @compliance_id, menu_name = 'route.compliance_dev-log',
    component = 'compliance/dev-log/index', path = 'dev-log', order_num = 1, menu_type = 'C',
    update_by = 1, update_time = NOW()
WHERE parent_id = @compliance_id AND path = 'dev-log';


-- ============================================================================
-- 3. 按钮权限（F 类型）—— 与后端 @SaCheckPermission 一一对应
--    append-only：只建「查询 / 新增 / 修改」，**不建「删除」**
-- ============================================================================
SET @dev_log_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @compliance_id AND path = 'dev-log' LIMIT 1);

-- 查询
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @dev_log_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'compliance:devLog:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @dev_log_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @dev_log_id AND perms = 'compliance:devLog:query') AS exists_check);

-- 新增
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @dev_log_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'compliance:devLog:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @dev_log_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @dev_log_id AND perms = 'compliance:devLog:add') AS exists_check);

-- 修改
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @dev_log_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'compliance:devLog:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @dev_log_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @dev_log_id AND perms = 'compliance:devLog:edit') AS exists_check);


-- ============================================================================
-- 4. 授权给超级管理员（sys_role_menu 主键是 (role_id, menu_id)，用 INSERT IGNORE 保证幂等）
--    设计矩阵里的 4 个业务角色（wet_lab / bioinformatician / interpreter / operation_admin）
--    本机不存在（只有 superadmin/test1/test2），故先只授 superadmin。
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id IN (@compliance_id, @dev_log_id)
   OR m.parent_id = @dev_log_id;


-- ============================================================================
-- 5. 3Q 验证记录表 validation_record
--    依据：docs/context/设计文档/validation-record.md §1（PG/Prisma 写法，类型按 ai-rules/04 §7 映射）
--    无 del_flag：合规 append-only（ai-rules/04-db-schema.md §2）
--    file_name：本次验证的「验证文档」文件名（原文件名，附件存服务器固定目录；
--               配置项 compliance.validation-record.upload-dir），重名即替换、库里不存路径
-- ============================================================================
CREATE TABLE IF NOT EXISTS `validation_record` (
    id               bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',
    validation_type  varchar(20)  NOT NULL                          COMMENT '验证类型（IQ 安装确认 / OQ 运行确认 / PQ 性能确认）',
    title            varchar(200) NOT NULL                          COMMENT '验证标题',
    version          varchar(50)                                    COMMENT '版本号（本次验证对应的方案/镜像版本）',
    executed_by      varchar(80)                                    COMMENT '执行人',
    executed_date    date                                           COMMENT '执行日期',
    result           varchar(20)  NOT NULL DEFAULT 'passed'         COMMENT '结果（passed 通过 / failed 未通过 / na 不适用）',
    summary          text                                           COMMENT '验证摘要',
    file_name        varchar(255)                                   COMMENT '验证文档文件名（原文件名，附件存服务器固定目录）',

    create_by        bigint                                         COMMENT '创建者',
    create_time      datetime                                       COMMENT '创建时间',
    update_by        bigint                                         COMMENT '更新者',
    update_time      datetime                                       COMMENT '更新时间',

    tenant_id        varchar(20)  NOT NULL DEFAULT '000000'         COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 设计文档的 idx_validation_record_type / _date；规范要求 tenant_id 打头
    KEY idx_validation_record_type (tenant_id, validation_type),
    KEY idx_validation_record_date (tenant_id, executed_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='3Q 验证记录';


-- ============================================================================
-- 6. 菜单：合规管理 > 3Q 验证记录
--    component 两层：compliance/validation-records/index → 路由名 compliance_validation-records
--    perms 与 ValidationRecordController 的 @SaCheckPermission 逐字一致
-- ============================================================================
SET @compliance_id_vr = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'compliance' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.compliance_validation-records', @compliance_id_vr, 1, 'validation-records',
    'compliance/validation-records/index', 1, 0,
    'C', '0', '0', 'compliance:validationRecord:list', 'local-icon-my-task', 1, NOW(), '3Q 验证记录'
FROM (SELECT 1) AS dummy
WHERE @compliance_id_vr IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @compliance_id_vr AND path = 'validation-records') AS exists_check);

-- 收敛到当前定义（脚本可重复执行）
UPDATE sys_menu
SET parent_id = @compliance_id_vr, menu_name = 'route.compliance_validation-records',
    component = 'compliance/validation-records/index', path = 'validation-records',
    order_num = 1, menu_type = 'C', update_by = 1, update_time = NOW()
WHERE parent_id = @compliance_id_vr AND path = 'validation-records';

-- 菜单顺序按母计划：3Q 验证记录在前、开发记录在后（只改 order_num，menu_id 不动，授权不受影响）
UPDATE sys_menu SET order_num = 2, update_by = 1, update_time = NOW()
WHERE parent_id = @compliance_id_vr AND path = 'dev-log';

SET @vr_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @compliance_id_vr AND path = 'validation-records' LIMIT 1);

-- F 按钮：查询 / 新增 / 修改（append-only，**不建「删除」**）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @vr_menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'compliance:validationRecord:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @vr_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @vr_menu_id AND perms = 'compliance:validationRecord:query') AS exists_check);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @vr_menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'compliance:validationRecord:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @vr_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @vr_menu_id AND perms = 'compliance:validationRecord:add') AS exists_check);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @vr_menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'compliance:validationRecord:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @vr_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @vr_menu_id AND perms = 'compliance:validationRecord:edit') AS exists_check);


-- ============================================================================
-- 7. 授权给超级管理员（设计矩阵里的 4 个业务角色本机不存在，先只授 superadmin）
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @vr_menu_id OR m.parent_id = @vr_menu_id;


-- ============================================================================
-- 8. 3Q 文档管理表 compliance_document
--    依据：docs/context/设计文档/compliance-docs.md §1（PG/Prisma 写法，类型按 ai-rules/04 §7 映射）
--    **记录类**：带 del_flag（0 存在 / 1 删除）+ 实体 @TableLogic，删除是「作废」语义、可恢复
--    file_name：文档文件名（原文件名，附件存服务器固定目录；配置项 compliance.compliance-doc.upload-dir）
--    设计文档里的 file_url（dataloom 存储地址）不落库：本仓不接 dataloom，附件按「固定目录 + 文件名」约定
-- ============================================================================
CREATE TABLE IF NOT EXISTS `compliance_document` (
    id            bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',
    doc_type      varchar(20)  NOT NULL                          COMMENT '文档类型（IQ 安装确认 / OQ 运行确认 / PQ 性能确认 / DEV_TEST 开发测试）',
    title         varchar(200) NOT NULL                          COMMENT '文档标题',
    version       varchar(50)  NOT NULL                          COMMENT '版本号',
    file_name     varchar(255)                                   COMMENT '文档文件名（原文件名，附件存服务器固定目录）',
    remark        varchar(500)                                   COMMENT '备注',

    create_by     bigint                                         COMMENT '创建者',
    create_time   datetime                                       COMMENT '创建时间',
    update_by     bigint                                         COMMENT '更新者',
    update_time   datetime                                       COMMENT '更新时间',
    del_flag      char(1)      NOT NULL DEFAULT '0'              COMMENT '删除标志（0代表存在 1代表删除）',

    tenant_id     varchar(20)  NOT NULL DEFAULT '000000'         COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 设计文档的 idx_compliance_doc_type；规范要求 tenant_id 打头
    KEY idx_compliance_doc_type (tenant_id, doc_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='3Q 文档管理';


-- ============================================================================
-- 9. 菜单：合规管理 > 3Q 文档管理
--    component 两层：compliance/documents/index → 路由名 compliance_documents
--    perms 与 ComplianceDocController 的 @SaCheckPermission 逐字一致
-- ============================================================================
SET @compliance_id_doc = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'compliance' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.compliance_documents', @compliance_id_doc, 3, 'documents',
    'compliance/documents/index', 1, 0,
    'C', '0', '0', 'compliance:complianceDoc:list', 'local-icon-clipboard', 1, NOW(), '3Q 文档管理'
FROM (SELECT 1) AS dummy
WHERE @compliance_id_doc IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @compliance_id_doc AND path = 'documents') AS exists_check);

-- 收敛到当前定义（脚本可重复执行）
UPDATE sys_menu
SET parent_id = @compliance_id_doc, menu_name = 'route.compliance_documents',
    component = 'compliance/documents/index', path = 'documents',
    order_num = 3, menu_type = 'C', update_by = 1, update_time = NOW()
WHERE parent_id = @compliance_id_doc AND path = 'documents';

SET @doc_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @compliance_id_doc AND path = 'documents' LIMIT 1);

-- F 按钮：查询 / 新增 / 修改 / 删除（记录类，**有删除**）
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @doc_menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'compliance:complianceDoc:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @doc_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @doc_menu_id AND perms = 'compliance:complianceDoc:query') AS exists_check);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @doc_menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'compliance:complianceDoc:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @doc_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @doc_menu_id AND perms = 'compliance:complianceDoc:add') AS exists_check);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @doc_menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'compliance:complianceDoc:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @doc_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @doc_menu_id AND perms = 'compliance:complianceDoc:edit') AS exists_check);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', @doc_menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'compliance:complianceDoc:remove', '#', 1, NOW(), '删除'
FROM (SELECT 1) AS dummy
WHERE @doc_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @doc_menu_id AND perms = 'compliance:complianceDoc:remove') AS exists_check);


-- ============================================================================
-- 10. 授权给超级管理员
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @doc_menu_id OR m.parent_id = @doc_menu_id;
