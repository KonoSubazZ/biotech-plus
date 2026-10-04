-- 开发记录表（dev_log）+「合规管理」菜单（一级目录）与「开发记录」页面、权限点位
-- 依据：docs/context/设计文档/dev-log.md
--       原文是 PG/Prisma 写法（uuid 主键、_created_at/_updated_at/_updated_by），
--       类型已按 ai-rules/04-db-schema.md §7 映射到本仓（bigint 自增 + 公共字段四件套）。
-- 合规口径：开发记录属「不可篡改的追溯证据」→ append-only，**不加 del_flag**、
--       不建「删除」按钮、后端不提供删除接口（ai-rules/04-db-schema.md §2「谁加 del_flag」）。
-- 规范：ai-rules/04-db-schema.md      模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py（error 必须为 0）
-- 说明：「合规管理」下还有「3Q 验证记录 / 3Q 文档管理」两页，后续实现时把各自的建表与菜单
--       追加到本文件（一个菜单模块一个文件），不要另起并列 SQL。
-- 菜单位置：「合规管理」是根目录下的一级菜单，页面路由 /compliance/dev-log

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
