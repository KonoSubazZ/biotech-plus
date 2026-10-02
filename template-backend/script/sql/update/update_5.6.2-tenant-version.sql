-- Run against the rbac-only database. Additive and repeatable; existing records belong to 000000.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS sys_tenant (
    id bigint NOT NULL,
    tenant_id varchar(20) NOT NULL,
    tenant_name varchar(100) NOT NULL,
    status char(1) NOT NULL DEFAULT '0',
    remark varchar(500) DEFAULT NULL,
    create_by bigint DEFAULT NULL,
    create_time datetime DEFAULT NULL,
    update_by bigint DEFAULT NULL,
    update_time datetime DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_tenant_identifier (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户目录';
INSERT IGNORE INTO sys_tenant (id, tenant_id, tenant_name, status, create_by, create_time)
VALUES (1, '000000', '默认租户', '0', 1, NOW());

DROP PROCEDURE IF EXISTS add_tenant_scope;
DELIMITER $$
CREATE PROCEDURE add_tenant_scope()
BEGIN
    DECLARE done int DEFAULT 0;
    DECLARE scope_table varchar(64);
    DECLARE tables_cursor CURSOR FOR
        SELECT TABLE_NAME FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
          AND TABLE_NAME NOT IN ('sys_tenant', 'sys_menu', 'sys_role', 'sys_role_menu', 'sys_client',
              'sys_config', 'sys_dict_type', 'sys_dict_data', 'sys_oss_config', 'gen_table', 'gen_table_column')
          AND LEFT(TABLE_NAME, 3) <> 'sj_';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    OPEN tables_cursor;
    scope_loop: LOOP
        FETCH tables_cursor INTO scope_table;
        IF done = 1 THEN LEAVE scope_loop; END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = scope_table AND COLUMN_NAME = 'tenant_id') THEN
            SET @scope_sql = CONCAT('ALTER TABLE `', scope_table,
                '` ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号''');
            PREPARE scope_statement FROM @scope_sql;
            EXECUTE scope_statement;
            DEALLOCATE PREPARE scope_statement;
        END IF;
        IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
            AND TABLE_NAME = scope_table AND COLUMN_NAME = 'tenant_id' AND SEQ_IN_INDEX = 1) THEN
            SET @scope_sql = CONCAT('ALTER TABLE `', scope_table, '` ADD INDEX idx_tenant_id (tenant_id)');
            PREPARE scope_statement FROM @scope_sql;
            EXECUTE scope_statement;
            DEALLOCATE PREPARE scope_statement;
        END IF;
    END LOOP;
    CLOSE tables_cursor;
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'sys_user' AND INDEX_NAME = 'uk_sys_user_login') THEN
        ALTER TABLE sys_user ADD UNIQUE INDEX uk_sys_user_login (user_name);
    END IF;
END$$
DELIMITER ;
CALL add_tenant_scope();
DROP PROCEDURE add_tenant_scope;

-- User-role rows always share the user's tenant, including pre-existing data.
UPDATE sys_user_role ur JOIN sys_user u ON ur.user_id = u.user_id SET ur.tenant_id = u.tenant_id;

-- Only the platform administrator receives this menu. order_num=0 puts it first in System Management.
SET @system_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'system' LIMIT 1);
SET @tenant_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @system_menu_id AND path = 'tenant' LIMIT 1);
SET @tenant_menu_id = IFNULL(@tenant_menu_id, (SELECT COALESCE(MAX(menu_id), 0) + 1 FROM sys_menu));
INSERT INTO sys_menu (menu_id, menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT @tenant_menu_id, 'route.system_tenant', @system_menu_id, 0, 'tenant', 'system/tenant/index', 1, 0,
    'C', '0', '0', 'system:tenant:list', 'mdi:office-building-outline', 1, NOW(), '租户数据隔离管理'
WHERE @system_menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_id = @tenant_menu_id);
UPDATE sys_menu SET order_num = 0 WHERE menu_id = @tenant_menu_id;
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, @tenant_menu_id WHERE @system_menu_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM sys_role_menu WHERE role_id = 1 AND menu_id = @tenant_menu_id);
