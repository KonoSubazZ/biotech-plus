-- ============================================================================
-- 业务表建表模板（照抄：改 <占位符> 和业务字段）
--   规则依据：ai-rules/04-db-schema.md
--   机械闸门：python3 tools/check_db_schema.py        （error 必须为 0）
-- ============================================================================
-- 步骤：
--   1. 复制本文件 → template-backend/script/sql/business/<模块>.sql
--   2. 全局替换：<模块>   → 菜单模块前缀（qc / product / company / audit / compliance / data / sample / dev / validation）
--                <实体>   → 表名后半段（record / standard / policy / log ...）
--                <中文名> → 页面/菜单上显示的中文
--   3. 按 ai-rules/04-db-schema.md §7 的映射表补业务字段（文档里是 PG/Prisma 写法，别照抄类型）
--   4. 执行（走文件，不要在 shell 里拼反引号）：
--        docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--          mysql -uroot --default-character-set=utf8mb4 ry-vue < <模块>.sql
--   5. 体检：python3 tools/check_db_schema.py
-- ============================================================================

CREATE TABLE IF NOT EXISTS `<模块>_<实体>` (
    -- ── 主键：单列 bigint 自增（全局 idType=AUTO；主键列不自增会报
    --    "Field 'id' doesn't have a default value"，见 04-db-schema §0.2 / §3）
    id                bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',

    -- ── 业务字段（示例，按实际删改）
    -- name           varchar(120) NOT NULL                           COMMENT '名称',
    -- code           varchar(60)  NOT NULL                           COMMENT '编码',
    -- status         varchar(20)  NOT NULL DEFAULT 'active'          COMMENT '状态（active 启用 / inactive 停用）',
    -- remark         varchar(500)                                    COMMENT '备注',

    -- ── 公共字段（每张业务表必备；注意不要加 create_dept —— sys_dept 已删，部门由租户替代）
    create_by         bigint                                         COMMENT '创建者',
    create_time       datetime                                       COMMENT '创建时间',
    update_by         bigint                                         COMMENT '更新者',
    update_time       datetime                                       COMMENT '更新时间',

    -- del_flag：业务"记录类"表保留；日志/审计类表（audit_log / dev_log / validation_record /
    --           data_archive_record）请删掉这一行 —— 这类表 append-only，不允许删改
    del_flag          char(1)      NOT NULL DEFAULT '0'              COMMENT '删除标志（0代表存在 1代表删除）',

    -- ── 租户：所有业务表必备。租户拦截器按它隔离；缺这一列，整个模块读写直接抛
    --    Unknown column 'tenant_id'（fail-fast）
    tenant_id         varchar(20)  NOT NULL DEFAULT '000000'         COMMENT '租户编号',

    PRIMARY KEY (id),

    -- 隔离表必备，命名固定 idx_tenant_id（与既有 7 张隔离表一致）
    KEY idx_tenant_id (tenant_id)

    -- ── 业务唯一键：tenant_id 必须打头。否则 A 租户占了 code=ABC，B 租户就建不了同编码
    -- ,UNIQUE KEY uk_<模块>_<实体>_code (tenant_id, code)

    -- ── 按查询需要加的普通索引（不要给 status / del_flag 这类低基数列单独建索引）
    -- ,KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='<中文名>';


-- ============================================================================
-- 附：复合主键表（多对多关系表）的写法 —— 主键列**不自增**
--     对应实体必须写 @TableId(type = IdType.INPUT) 显式传值，参考 SysUserRole / SysRoleMenu
-- ============================================================================
-- CREATE TABLE IF NOT EXISTS `<模块>_<实体>_rel` (
--     <a>_id            bigint      NOT NULL COMMENT 'A 主键',
--     <b>_id            bigint      NOT NULL COMMENT 'B 主键',
--     create_by         bigint               COMMENT '创建者',
--     create_time       datetime             COMMENT '创建时间',
--     tenant_id         varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
--     PRIMARY KEY (<a>_id, <b>_id),
--     KEY idx_tenant_id (tenant_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='<中文名>';
