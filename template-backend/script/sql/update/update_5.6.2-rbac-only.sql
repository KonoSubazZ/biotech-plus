-- RuoYi-Vue-Plus 5.6.2 -> 单实例 RBAC 基线迁移
-- 适用：MySQL 5.7+
-- 说明：DDL 会隐式提交。执行前请完成数据库备份，并确认没有非默认租户数据。

SET NAMES utf8mb4;

-- 本版本不提供跨租户数据合并。发现非默认租户时主动终止，避免误删数据。
DROP PROCEDURE IF EXISTS assert_default_tenant;
DELIMITER $$
CREATE PROCEDURE assert_default_tenant()
BEGIN
  DECLARE v_table_exists INT DEFAULT 0;
  DECLARE v_invalid_rows INT DEFAULT 0;
  SELECT COUNT(*) INTO v_table_exists
    FROM information_schema.tables
   WHERE table_schema = DATABASE() AND table_name = 'sys_tenant' AND table_type = 'BASE TABLE';
  IF v_table_exists > 0 THEN
    SELECT COUNT(*) INTO v_invalid_rows FROM sys_tenant WHERE tenant_id <> '000000';
    IF v_invalid_rows > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Migration stopped: non-default tenant data exists';
    END IF;
  END IF;
END$$
DELIMITER ;
CALL assert_default_tenant();
DROP PROCEDURE IF EXISTS assert_default_tenant;

-- 旧基线中的演示角色名称来自数据权限场景，迁移后改为普通 RBAC 角色名称。
UPDATE sys_role
   SET role_name = '普通用户'
 WHERE role_id = 3
   AND role_key = 'test1';
UPDATE sys_role
   SET role_name = '业务用户'
 WHERE role_id = 4
   AND role_key = 'test2';

-- 动态路由来自 sys_menu，删除已下线功能及其按钮权限，避免前端加载不存在的页面。
DROP PROCEDURE IF EXISTS remove_obsolete_menus;
DELIMITER $$
CREATE PROCEDURE remove_obsolete_menus()
BEGIN
  DECLARE v_added INT DEFAULT 1;
  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_obsolete_menu_ids (menu_id BIGINT PRIMARY KEY);
  CREATE TEMPORARY TABLE IF NOT EXISTS tmp_new_obsolete_menu_ids (menu_id BIGINT PRIMARY KEY);
  INSERT IGNORE INTO tmp_obsolete_menu_ids(menu_id)
    SELECT menu_id FROM sys_menu
     WHERE path IN ('demo', 'dept', 'post', 'tenant', 'tenant-package', 'workflow')
        OR perms LIKE 'system:dept:%' OR perms LIKE 'system:post:%'
        OR perms LIKE 'system:tenant:%' OR perms LIKE 'demo:%' OR perms LIKE 'flow:%';
  WHILE v_added > 0 DO
    TRUNCATE TABLE tmp_new_obsolete_menu_ids;
    INSERT IGNORE INTO tmp_new_obsolete_menu_ids(menu_id)
      SELECT sm.menu_id FROM sys_menu sm
       INNER JOIN tmp_obsolete_menu_ids x ON x.menu_id = sm.parent_id;
    INSERT IGNORE INTO tmp_obsolete_menu_ids(menu_id)
      SELECT menu_id FROM tmp_new_obsolete_menu_ids;
    SET v_added = ROW_COUNT();
  END WHILE;
  DELETE rm FROM sys_role_menu rm INNER JOIN tmp_obsolete_menu_ids x ON x.menu_id = rm.menu_id;
  DELETE sm FROM sys_menu sm INNER JOIN tmp_obsolete_menu_ids x ON x.menu_id = sm.menu_id;
  DROP TEMPORARY TABLE IF EXISTS tmp_obsolete_menu_ids;
  DROP TEMPORARY TABLE IF EXISTS tmp_new_obsolete_menu_ids;
END$$
DELIMITER ;
CALL remove_obsolete_menus();
DROP PROCEDURE IF EXISTS remove_obsolete_menus;

-- 删除不再由 RBAC 使用的组织、岗位、租户和演示表。
DROP TABLE IF EXISTS sys_user_post;
DROP TABLE IF EXISTS sys_role_dept;
DROP TABLE IF EXISTS sys_post;
DROP TABLE IF EXISTS sys_dept;
DROP TABLE IF EXISTS sys_tenant_package;
DROP TABLE IF EXISTS sys_tenant;
DROP TABLE IF EXISTS test_demo;
DROP TABLE IF EXISTS test_tree;
DROP TABLE IF EXISTS flow_instance_biz_ext;
DROP TABLE IF EXISTS flow_his_task;
DROP TABLE IF EXISTS flow_task;
DROP TABLE IF EXISTS flow_instance;
DROP TABLE IF EXISTS flow_skip;
DROP TABLE IF EXISTS flow_node;
DROP TABLE IF EXISTS flow_definition;
DROP TABLE IF EXISTS flow_user;
DROP TABLE IF EXISTS flow_category;
DROP TABLE IF EXISTS flow_spel;
DROP TABLE IF EXISTS test_leave;

-- 5.7 没有 DROP COLUMN IF EXISTS，使用信息_SCHEMA 动态生成安全 DDL。
DROP PROCEDURE IF EXISTS drop_column_if_exists;
DELIMITER $$
CREATE PROCEDURE drop_column_if_exists(IN p_table VARCHAR(64), IN p_column VARCHAR(64))
BEGIN
  DECLARE v_exists INT DEFAULT 0;
  DECLARE v_column_count INT DEFAULT 0;
  SELECT COUNT(*) INTO v_exists
    FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column;
  SELECT COUNT(*) INTO v_column_count
    FROM information_schema.columns
   WHERE table_schema = DATABASE() AND table_name = p_table;
  IF v_exists > 0 AND v_column_count > 1 THEN
    SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
    PREPARE s FROM @ddl;
    EXECUTE s;
    DEALLOCATE PREPARE s;
  END IF;
END$$
DELIMITER ;

CALL drop_column_if_exists('sys_social', 'tenant_id');
CALL drop_column_if_exists('sys_user', 'tenant_id');
CALL drop_column_if_exists('sys_role', 'tenant_id');
CALL drop_column_if_exists('sys_oper_log', 'tenant_id');
CALL drop_column_if_exists('sys_dict_type', 'tenant_id');
CALL drop_column_if_exists('sys_dict_data', 'tenant_id');
CALL drop_column_if_exists('sys_config', 'tenant_id');
CALL drop_column_if_exists('sys_logininfor', 'tenant_id');
CALL drop_column_if_exists('sys_notice', 'tenant_id');
CALL drop_column_if_exists('sys_oss', 'tenant_id');
CALL drop_column_if_exists('sys_oss_config', 'tenant_id');
CALL drop_column_if_exists('sys_client', 'tenant_id');
CALL drop_column_if_exists('sys_menu', 'tenant_id');
CALL drop_column_if_exists('sys_user', 'dept_id');

CALL drop_column_if_exists('sys_social', 'create_dept');
CALL drop_column_if_exists('sys_user', 'create_dept');
CALL drop_column_if_exists('sys_role', 'create_dept');
CALL drop_column_if_exists('sys_oper_log', 'create_dept');
CALL drop_column_if_exists('sys_dict_type', 'create_dept');
CALL drop_column_if_exists('sys_dict_data', 'create_dept');
CALL drop_column_if_exists('sys_config', 'create_dept');
CALL drop_column_if_exists('sys_logininfor', 'create_dept');
CALL drop_column_if_exists('sys_notice', 'create_dept');
CALL drop_column_if_exists('sys_oss', 'create_dept');
CALL drop_column_if_exists('sys_oss_config', 'create_dept');
CALL drop_column_if_exists('sys_client', 'create_dept');
CALL drop_column_if_exists('sys_menu', 'create_dept');
CALL drop_column_if_exists('sys_role', 'data_scope');
CALL drop_column_if_exists('sys_role', 'dept_check_strictly');
CALL drop_column_if_exists('gen_table', 'create_dept');
CALL drop_column_if_exists('gen_table_column', 'create_dept');

DROP PROCEDURE IF EXISTS drop_column_if_exists;

-- 字典类型原来的唯一键包含 tenant_id，迁移后恢复为单实例唯一键。
SET @has_old_dict_index := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'sys_dict_type'
     AND index_name = 'dict_type'
);
SET @sql := IF(@has_old_dict_index > 0,
  'ALTER TABLE sys_dict_type DROP INDEX dict_type',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
SET @has_new_dict_index := (
  SELECT COUNT(*) FROM information_schema.statistics
   WHERE table_schema = DATABASE() AND table_name = 'sys_dict_type'
     AND index_name = 'uk_sys_dict_type'
);
SET @sql := IF(@has_new_dict_index = 0,
  'ALTER TABLE sys_dict_type ADD UNIQUE KEY uk_sys_dict_type (dict_type)',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
