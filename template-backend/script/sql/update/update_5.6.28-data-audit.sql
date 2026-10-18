-- 数据审计：只追加事件与实际记录变化；可重复执行；不回填虚构历史。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS audit_event (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '事件主键',
    tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '业务所属药企；平台事件为__platform__',
    operation_id char(32) NOT NULL COMMENT '同事务业务操作标识',
    audit_scope varchar(16) NOT NULL COMMENT 'SAMPLE样本相关 NON_SAMPLE非样本',
    action varchar(100) NOT NULL COMMENT '实际业务动作',
    subbarcode varchar(160) DEFAULT NULL COMMENT '全局唯一样本编号；非样本事件为空',
    analysis_id bigint DEFAULT NULL COMMENT '分析数据批次；样本公共事件为空',
    report_id bigint DEFAULT NULL COMMENT '实际报告记录',
    actor_id varchar(100) NOT NULL COMMENT '操作者稳定标识或SYSTEM',
    actor_name varchar(120) NOT NULL COMMENT '当时操作者名称',
    source varchar(24) NOT NULL COMMENT 'USER或SERVICE',
    outcome varchar(24) NOT NULL COMMENT 'STARTED SUCCESS FAILED NO_CHANGE',
    occurred_at datetime(3) NOT NULL COMMENT '服务端操作时间',
    change_count int NOT NULL DEFAULT 0 COMMENT '实际记录变化数量',
    create_by bigint DEFAULT NULL COMMENT '创建者',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by bigint DEFAULT NULL COMMENT '更新者',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_audit_sample_time (tenant_id, subbarcode, occurred_at, id),
    KEY idx_audit_analysis_time (tenant_id, analysis_id, occurred_at, id),
    KEY idx_audit_scope_time (tenant_id, audit_scope, occurred_at, id),
    KEY idx_audit_operation (tenant_id, operation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='只追加数据审计事件';

CREATE TABLE IF NOT EXISTS audit_change (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '变化明细主键',
    tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '与父事件相同的业务归属',
    event_id bigint NOT NULL COMMENT '审计事件主键；不级联删除',
    table_name varchar(64) NOT NULL COMMENT '实际业务表名',
    object_key varchar(500) NOT NULL COMMENT '真实主键JSON；关联表保留完整复合键',
    change_type varchar(24) NOT NULL COMMENT 'CREATE UPDATE DELETE SOFT_DELETE RESTORE',
    before_json longtext DEFAULT NULL COMMENT '受控前值；新增为空',
    after_json longtext DEFAULT NULL COMMENT '受控后值；物理删除为空',
    changed_fields_json longtext NOT NULL COMMENT '字段名及前后值JSON；不保存凭证',
    create_by bigint DEFAULT NULL COMMENT '创建者',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by bigint DEFAULT NULL COMMENT '更新者',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_audit_change_event (tenant_id, event_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='只追加逐记录审计变化';

SET @audit_parent = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'compliance' LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.compliance_data-audit', @audit_parent, 4, 'data-audit', 'compliance/data-audit/index', 1, 0,
    'C', '0', '0', 'compliance:dataAudit:list', 'local-icon-clipboard', 1, NOW(), '数据审计'
FROM (SELECT 1) AS seed
WHERE @audit_parent IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @audit_parent AND path = 'data-audit');

SET @audit_menu = (SELECT menu_id FROM sys_menu WHERE parent_id = @audit_parent AND path = 'data-audit' LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '审计详情', @audit_menu, 1, '', NULL, 1, 0,
    'F', '0', '0', 'compliance:dataAudit:query', '#', 1, NOW(), '查看审计前后值'
FROM (SELECT 1) AS seed
WHERE @audit_menu IS NOT NULL AND NOT EXISTS (
    SELECT 1 FROM sys_menu WHERE parent_id = @audit_menu AND perms = 'compliance:dataAudit:query');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE menu_id = @audit_menu OR parent_id = @audit_menu;
