-- 开发记录（dev_log）加字段：版本号 version、记录文档 file_name
-- 为什么：「合规管理 › 开发记录」页要求展示「版本」与「记录文档」（只显示上传文件名、可点击下载），
--        附件按用户要求先落在服务器固定目录（配置项 compliance.dev-log.upload-dir），
--        库里只记原始文件名 —— 重名即替换，所以不存路径、也不允许多份同名文件。
-- 幂等：先查 information_schema 再 ALTER（MySQL 5.7 没有 ADD COLUMN IF NOT EXISTS），可反复执行。
-- 同步：template-backend/script/sql/business/compliance.sql 的列定义要一起改（新环境重建才是对的）。
-- 落地：deploy.sh up 的 mysql/init 只在重建数据卷时执行 → 已存在的库要手工跑一次本脚本。
SET NAMES utf8mb4;

SET @add_version = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `dev_log` ADD COLUMN `version` varchar(50) NULL COMMENT ''版本号'' AFTER `category`',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dev_log' AND COLUMN_NAME = 'version');
PREPARE stmt_version FROM @add_version;
EXECUTE stmt_version;
DEALLOCATE PREPARE stmt_version;

SET @add_file_name = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `dev_log` ADD COLUMN `file_name` varchar(255) NULL COMMENT ''记录文档文件名（原文件名，附件存服务器固定目录）'' AFTER `content`',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dev_log' AND COLUMN_NAME = 'file_name');
PREPARE stmt_file_name FROM @add_file_name;
EXECUTE stmt_file_name;
DEALLOCATE PREPARE stmt_file_name;
