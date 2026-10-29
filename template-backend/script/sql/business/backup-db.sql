-- =====================================================================================
-- 数据备份基线：数据库全量 + binlog 增量（新环境按序导入）
-- 与 script/sql/update/update_5.6.44-db-backup.sql 的表结构保持一致；
-- 建表语句是唯一来源，改结构时两处同步（update 脚本负责存量环境，本文件负责新环境）。
-- 统一前缀 backup_；备份相关的新表一律沿用该前缀。
-- =====================================================================================

USE `ry-vue`;
SET NAMES utf8mb4;

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
