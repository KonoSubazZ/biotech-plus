SET NAMES utf8mb4;

-- ============================================================================
-- 报告管理 › 邮件配置：从「一个租户一条」改成「多条发件邮箱 + 一条默认」
--
-- 为什么：
--   一个发件邮箱不够用（不同业务线/客户可能用不同发件邮箱）。配置页要变成列表，
--   每行一套账号密码/标题正文，并用「是否默认」标记报告发送页默认选中的那条。
--   所以要去掉「租户唯一」约束（uk_report_mail_config_tenant），加 is_default 列。
--
-- 幂等：MySQL 5.7 无 ADD COLUMN/DROP INDEX IF EXISTS，一律先查 information_schema 再动态执行。
-- 只增不改：本文件只做上面两件事 + 把已有那行补成默认。
-- ============================================================================

-- 1) 加「是否默认」列
SET @has_is_default = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'report_mail_config' AND column_name = 'is_default');
SET @ddl_add = IF(@has_is_default = 0,
    'ALTER TABLE `report_mail_config` ADD COLUMN `is_default` char(1) NOT NULL DEFAULT ''0'' COMMENT ''是否默认发件邮箱（0否 1是；同一租户只允许一条为 1）'' AFTER `enabled`',
    'DO 0');
PREPARE stmt_add FROM @ddl_add;
EXECUTE stmt_add;
DEALLOCATE PREPARE stmt_add;

-- 2) 去掉「一个租户一条」的唯一键（现在要能配多条）
SET @has_uk = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE() AND table_name = 'report_mail_config'
      AND index_name = 'uk_report_mail_config_tenant');
SET @ddl_drop = IF(@has_uk > 0,
    'ALTER TABLE `report_mail_config` DROP INDEX `uk_report_mail_config_tenant`',
    'DO 0');
PREPARE stmt_drop FROM @ddl_drop;
EXECUTE stmt_drop;
DEALLOCATE PREPARE stmt_drop;

-- 3) 已有数据兜底：一条默认都没有时，把最早那条（id 最小）置为默认
SET @has_any = (SELECT COUNT(*) FROM `report_mail_config` WHERE del_flag = '0');
SET @has_default = (SELECT COUNT(*) FROM `report_mail_config` WHERE del_flag = '0' AND is_default = '1');
SET @first_id = (SELECT id FROM `report_mail_config` WHERE del_flag = '0' ORDER BY id LIMIT 1);
SET @ddl_default = IF(@has_any > 0 AND @has_default = 0,
    CONCAT('UPDATE `report_mail_config` SET is_default = ''1'' WHERE id = ', IFNULL(@first_id, 0)),
    'DO 0');
PREPARE stmt_default FROM @ddl_default;
EXECUTE stmt_default;
DEALLOCATE PREPARE stmt_default;
