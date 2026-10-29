-- =====================================================================================
-- 数据备份模块：数据库全量备份 + binlog 增量归档
--
-- 为什么（用户 2026-10-29）：
--   1. 备份策略、库账号密码、OSS 全在宿主机的 backup/conf/backup.yml（Python 脚本读），库里不存策略；
--   2. 系统只做「展示」：脚本跑成功后通过接口上报一条事实记录，页面只读，不提供任何触发/删除/改策略操作；
--   3. 本期不处理失败：脚本只在成功时上报，所以表里没有 fail_reason / verify_status 之类列；
--      备份相关的表统一 backup_ 前缀。
--   4. 脚本上报不改用全新鉴权：复用仓库既有的 X-Api-Token（api.token），本脚本用专用账号 sys_backup，
--      只授予 backup:db:report 一个权限点位。
--
-- 幂等：每步都先查再动（表用 IF NOT EXISTS、菜单/角色/账号用 NOT EXISTS、授权用 INSERT IGNORE），可重复执行。
-- =====================================================================================

USE `ry-vue`;
SET NAMES utf8mb4;

-- -------------------------------------------------------------------------------------
-- 1. 两张事实表
-- -------------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `backup_db_full` (
  `id`              bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance`        varchar(64)   NOT NULL DEFAULT 'biotech-plus-mysql' COMMENT '实例标识',
  `db_name`         varchar(64)   NOT NULL COMMENT '库名',
  `backup_date`     date          NOT NULL COMMENT '备份日期',
  `backup_type`     varchar(16)   NOT NULL DEFAULT 'weekly' COMMENT '类型：weekly 周定 / manual 手工补跑',
  `excluded_tables` varchar(1000) NOT NULL DEFAULT '' COMMENT '本份 dump 排除的表（脚本按配置展开后的实际表名，逗号分隔）',
  `table_count`     int(11)       DEFAULT NULL COMMENT 'dump 内 CREATE TABLE 数（已扣除排除表）',
  `db_table_total`  int(11)       DEFAULT NULL COMMENT '该库实际表数（对账：table_count + 排除数）',
  `binlog_file`     varchar(64)   DEFAULT NULL COMMENT '基线 binlog 文件（mysqldump --master-data=2 记录）',
  `binlog_pos`      bigint(20)    DEFAULT NULL COMMENT '基线位点（PITR 起点）',
  `local_path`      varchar(1024) DEFAULT NULL COMMENT '本地压缩包路径',
  `local_md5`       varchar(32)   DEFAULT NULL COMMENT '本地压缩包 MD5',
  `local_size`      bigint(20)    DEFAULT NULL COMMENT '压缩包字节数',
  `local_pruned`    tinyint(4)    NOT NULL DEFAULT 0 COMMENT '本地是否已按保留策略清理（0 在 / 1 已清，OSS 仍在）',
  `oss_path`        varchar(1024) DEFAULT NULL COMMENT 'OSS 对象路径（oss.enabled=false 时为空）',
  `oss_meta_md5`    varchar(32)   DEFAULT NULL COMMENT 'OSS x-oss-meta-file-md5 回读值',
  `duration_ms`     bigint(20)    DEFAULT NULL COMMENT '全流程耗时（毫秒）',
  `config_hash`     char(40)      DEFAULT NULL COMMENT '执行时 backup.yml 的 sha1（溯源用）',
  `script_version`  varchar(32)   DEFAULT NULL COMMENT '脚本 git 短 commit（溯源用）',
  `backup_status`   tinyint(4)    NOT NULL DEFAULT 2 COMMENT '预留状态：0 待 / 1 中 / 2 成功（本期只写 2）',
  `started_at`      datetime      DEFAULT NULL COMMENT '开始时间',
  `finished_at`     datetime      DEFAULT NULL COMMENT '结束时间',
  `create_by`       bigint(20)    DEFAULT NULL COMMENT '创建者（脚本上报为 sys_backup）',
  `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
  `update_by`       bigint(20)    DEFAULT NULL COMMENT '更新者',
  `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
  `tenant_id`       varchar(20)   NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bdf_db_date_type` (`db_name`, `backup_date`, `backup_type`),
  KEY `idx_bdf_date` (`backup_date`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='数据库全量备份记录';

CREATE TABLE IF NOT EXISTS `backup_db_binlog` (
  `id`              bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance`        varchar(64)   NOT NULL DEFAULT 'biotech-plus-mysql' COMMENT '实例标识',
  `backup_date`     date          NOT NULL COMMENT '归档日期',
  `db_scope`        varchar(200)  NOT NULL DEFAULT 'ry-vue,nkb' COMMENT '本包覆盖的库（恢复时 --database 过滤回放）',
  `start_file`      varchar(64)   NOT NULL COMMENT '起点 binlog 文件名（= 上一条 end 或全量基线）',
  `start_pos`       bigint(20)    NOT NULL COMMENT '起点位点',
  `end_file`        varchar(64)   NOT NULL COMMENT '终点 binlog 文件名（备份瞬间 SHOW MASTER STATUS）',
  `end_pos`         bigint(20)    NOT NULL COMMENT '终点位点',
  `file_count`      int(11)       NOT NULL DEFAULT 0 COMMENT 'binlog 文件个数',
  `total_size`      bigint(20)    NOT NULL DEFAULT 0 COMMENT '本包总字节',
  `files_json`      text          COMMENT '文件清单 JSON：[{file,size,md5}]',
  `local_dir`       varchar(1024) DEFAULT NULL COMMENT '本地目录',
  `local_pruned`    tinyint(4)    NOT NULL DEFAULT 0 COMMENT '本地是否已清理（0 在 / 1 已清）',
  `oss_prefix`      varchar(1024) DEFAULT NULL COMMENT 'OSS 前缀',
  `duration_ms`     bigint(20)    DEFAULT NULL COMMENT '耗时（毫秒）',
  `config_hash`     char(40)      DEFAULT NULL COMMENT '执行时 backup.yml 的 sha1',
  `script_version`  varchar(32)   DEFAULT NULL COMMENT '脚本 git 短 commit',
  `backup_status`   tinyint(4)    NOT NULL DEFAULT 2 COMMENT '预留状态：0 待 / 1 中 / 2 成功（本期只写 2）',
  `started_at`      datetime      DEFAULT NULL COMMENT '开始时间',
  `finished_at`     datetime      DEFAULT NULL COMMENT '完成时间',
  `create_by`       bigint(20)    DEFAULT NULL COMMENT '创建者（脚本上报为 sys_backup）',
  `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
  `update_by`       bigint(20)    DEFAULT NULL COMMENT '更新者',
  `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
  `tenant_id`       varchar(20)   NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bdb_inst_date` (`instance`, `backup_date`),
  KEY `idx_bdb_date` (`backup_date`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='binlog 增量归档记录';

-- 存量兜底：表已按老版本建过（缺 create_by / update_by）时补列，幂等
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'backup_db_binlog' AND COLUMN_NAME = 'create_by');
SET @s := IF(@c = 0, 'ALTER TABLE `backup_db_binlog` ADD COLUMN `create_by` bigint(20) DEFAULT NULL COMMENT ''创建者'' AFTER `finished_at`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'backup_db_binlog' AND COLUMN_NAME = 'update_by');
SET @s := IF(@c = 0, 'ALTER TABLE `backup_db_binlog` ADD COLUMN `update_by` bigint(20) DEFAULT NULL COMMENT ''更新者'' AFTER `create_time`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- -------------------------------------------------------------------------------------
-- 2. 菜单：数据备份（一级目录）→ 数据库备份（只读页面）→ 查询/上报两个按钮
-- -------------------------------------------------------------------------------------

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
                        `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 'route.backup', 0, 5, 'backup', 'Layout', 1, 0, 'M', '0', '0', '', 'carbon:data-backup', 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = 'backup' AND `parent_id` = 0 AND `menu_type` = 'M');

SET @dir := (SELECT `menu_id` FROM `sys_menu` WHERE `path` = 'backup' AND `parent_id` = 0 AND `menu_type` = 'M' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
                        `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT 'route.backup_database', @dir, 1, 'database', 'backup/database/index', 1, 0, 'C', '0', '0',
       'backup:db:list', 'carbon:data-table', 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `path` = 'database' AND `parent_id` = @dir AND `menu_type` = 'C');

SET @menu := (SELECT `menu_id` FROM `sys_menu` WHERE `path` = 'database' AND `parent_id` = @dir AND `menu_type` = 'C' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
                        `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT '查询', @menu, 1, '', '', 1, 0, 'F', '0', '0', 'backup:db:query', '', 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'backup:db:query');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
                        `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
SELECT '上报', @menu, 2, '', '', 1, 0, 'F', '0', '0', 'backup:db:report', '', 1, NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE `perms` = 'backup:db:report');

-- -------------------------------------------------------------------------------------
-- 3. 脚本专用角色 + 账号 sys_backup
--    密码 SysBackup@2026（bcrypt，RuoYi/hutool 兼容的 $2a$ 前缀）；上线后由运维自行改密
-- -------------------------------------------------------------------------------------

INSERT INTO `sys_role` (`role_name`, `role_key`, `role_sort`, `menu_check_strictly`, `status`, `del_flag`,
                        `create_by`, `create_time`, `remark`)
SELECT '备份脚本账号', 'backup_bot', 90, 1, '0', '0', 1, NOW(), '只授 backup:db:report，供宿主备份脚本以 X-Api-Token 上报'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_role` WHERE `role_key` = 'backup_bot' AND `del_flag` = '0');

SET @role := (SELECT `role_id` FROM `sys_role` WHERE `role_key` = 'backup_bot' AND `del_flag` = '0' LIMIT 1);

INSERT INTO `sys_user` (`user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `password`,
                        `status`, `del_flag`, `create_by`, `create_time`, `remark`, `tenant_id`)
SELECT 'sys_backup', '备份脚本账号', 'sys_user', '', '', '0',
       '$2a$10$40a45u66y1INKTXqKIj8zOmBLB0ITj3pLZcztYOVVsbZtfoniiaq.', '0', '0', 1, NOW(),
       '数据库备份脚本专用上报账号（X-Api-Token，不用于登录页面；密码 SysBackup@2026）', '000000'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `user_name` = 'sys_backup');

SET @uid := (SELECT `user_id` FROM `sys_user` WHERE `user_name` = 'sys_backup' LIMIT 1);

INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`) VALUES (@uid, @role);

-- -------------------------------------------------------------------------------------
-- 4. 授权
--    · 超级管理员：目录 + 页面 + 两个按钮（登录后台能看到这一页）
--    · 脚本账号：只授「目录 + 上报按钮」（目录是为了菜单树合法；不授页面 ⇒ 没有 list/query 权限）
-- -------------------------------------------------------------------------------------

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 1, `menu_id` FROM `sys_menu`
WHERE `menu_id` IN (@dir, @menu) OR `perms` IN ('backup:db:query', 'backup:db:report');

INSERT IGNORE INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT @role, `menu_id` FROM `sys_menu`
WHERE `menu_id` = @dir OR `perms` = 'backup:db:report';

-- 兜底收敛（本机实测踩到）：role_id 会被历史删除的角色复用 —— 本机 AUTO_INCREMENT 回到 9，
-- 而 9 号曾经是别的角色，`sys_role_menu` 里留着它 3 条孤儿授权（报告解读 list / enter / lims）。
-- 不清掉就等于给备份脚本账号开了报告解读的口子，所以这里把该角色授权收敛到本模块的两条。
DELETE FROM `sys_role_menu`
WHERE `role_id` = @role
  AND `menu_id` NOT IN (SELECT `menu_id` FROM `sys_menu`
                        WHERE `menu_id` = @dir OR `perms` = 'backup:db:report');

-- -------------------------------------------------------------------------------------
-- 5. 自检
-- -------------------------------------------------------------------------------------

SELECT 'tables' AS item, GROUP_CONCAT(TABLE_NAME ORDER BY TABLE_NAME) AS detail
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME LIKE 'backup\_%'
UNION ALL
SELECT 'menus', GROUP_CONCAT(CONCAT(menu_id, ':', menu_name, ':', menu_type) ORDER BY menu_id)
FROM `sys_menu` WHERE `menu_id` IN (@dir, @menu) OR `perms` LIKE 'backup:db:%'
UNION ALL
SELECT 'account', CONCAT('user_id=', @uid, ' role_id=', @role, ' grants=',
       (SELECT COUNT(*) FROM `sys_role_menu` WHERE `role_id` = @role))
UNION ALL
SELECT 'admin_grants', CAST((SELECT COUNT(*) FROM `sys_role_menu` WHERE `role_id` = 1
       AND `menu_id` IN (@dir, @menu)) AS CHAR);
