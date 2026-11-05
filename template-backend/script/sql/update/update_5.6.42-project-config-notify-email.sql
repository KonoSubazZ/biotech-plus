-- =====================================================================================
-- 项目配置：新增「项目通知收件邮箱」（project_notify_email）
--
-- 为什么（用户 2026-10-29）：项目配置页面上「项目收件邮箱」要隐藏（字段保留、数据不删），
--   另外新增一个「项目通知（收件）邮箱」，用于系统通知类邮件，与报告收件邮箱分开维护。
--   隐藏而不是删除：历史数据与 report-driver 取数仍按 receive_email 走，字段必须留在库里。
--
-- 存量数据：新列为空串，无需回填；通知邮箱不是必填项，未配置时不影响任何流程。
-- 幂等：先查 information_schema 再动作，可重复执行。
-- =====================================================================================

USE `ry-vue`;
SET NAMES utf8mb4;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND COLUMN_NAME = 'project_notify_email');
SET @s := IF(@c = 0,
  'ALTER TABLE `project_config` ADD COLUMN `project_notify_email` varchar(500) NOT NULL DEFAULT '''' COMMENT ''项目通知收件邮箱（;分隔；仅项目行使用，与报告收件邮箱分开）'' AFTER `receive_email`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 自检：列存在即可
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND COLUMN_NAME = 'project_notify_email';
