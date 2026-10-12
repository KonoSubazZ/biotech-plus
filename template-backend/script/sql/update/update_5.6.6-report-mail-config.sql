SET NAMES utf8mb4;

-- ============================================================================
-- 报告管理 › 邮件配置：报告发送用的腾讯企业邮箱配置（全局单条/租户）+ 菜单/权限
--
-- 为什么：
--   报告发送要给客户发「检测报告」邮件（附件 = 已生成的报告制品），
--   账号 / 密码 / 默认标题 / 默认正文不能写死在 application.yml（会跟着代码走、
--   且多租户下每个租户要能各自改）。所以落一张配置表，页面可改；
--   代码里只保留默认值（见 org.dromara.report.service.ReportMailDefaults）。
--
-- 口径来源：生产系统 report-system（src/mail.properties + EmailUtil.java）
--   SMTP smtp.exmail.qq.com:465 + SSL；发件昵称「诺禾致源报告」；
--   标题「请查收诺禾致源的检测报告，姓名：{{patientName}}-{{subbarcode}}」。
--
-- 幂等：CREATE TABLE IF NOT EXISTS；菜单按 path / perms 判 NOT EXISTS，可重复执行。
-- 注意：发送审计表 analysis_report_email_log 已由 business/report-interpretation.sql 建好，
--       本脚本**不重复创建**。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `report_mail_config` (
    `id`              bigint       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `enabled`         char(1)      NOT NULL DEFAULT '0' COMMENT '是否启用邮件发送（0否 1是）',
    `smtp_host`       varchar(120) NOT NULL DEFAULT 'smtp.exmail.qq.com' COMMENT 'SMTP 服务器',
    `smtp_port`       int          NOT NULL DEFAULT 465 COMMENT 'SMTP 端口',
    `ssl_enable`      char(1)      NOT NULL DEFAULT '1' COMMENT '是否 SSL（0否 1是）',
    `from_email`      varchar(200) DEFAULT NULL COMMENT '发件邮箱（= SMTP 登录账号）',
    `from_name`       varchar(120) DEFAULT NULL COMMENT '发件人昵称（如「诺禾致源报告」）',
    `password`        varchar(200) DEFAULT NULL COMMENT '邮箱密码 / 客户端授权码',
    `default_subject` varchar(500) DEFAULT NULL COMMENT '默认标题模板（支持 {{变量}}）',
    `default_content` text         COMMENT '默认正文模板（HTML，支持 {{变量}}；支持 <br>）',
    `create_by`       bigint       DEFAULT NULL COMMENT '创建者',
    `create_time`     datetime     DEFAULT NULL COMMENT '创建时间',
    `update_by`       bigint       DEFAULT NULL COMMENT '更新者',
    `update_time`     datetime     DEFAULT NULL COMMENT '更新时间',
    `del_flag`        char(1)      NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`       varchar(20)  NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_mail_config_tenant` (`tenant_id`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报告邮件发送配置（全局单条/租户）';


-- ============================================================================
-- 菜单：「报告管理」（父，path='report'）›「邮件配置」，order_num=5（现有 1..4 已占）
-- 幂等：按 path 定位父菜单（不写死 menu_id）；NOT EXISTS 包派生表（MySQL 5.7 无 LATERAL）
-- ============================================================================
SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.report_mail-config', @report_menu_id, 5, 'mail-config', 'report/mail-config/index',
    1, 0, 'C', '0', '0', 'report:mailConfig:list', 'local-icon-documentation', 1, NOW(), '邮件配置'
FROM (SELECT 1) AS dummy
WHERE @report_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @report_menu_id AND path = 'mail-config') AS c0);

SET @mail_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @report_menu_id AND path = 'mail-config' AND menu_type = 'C' LIMIT 1);

-- 按钮权限（F）：与 @SaCheckPermission 逐字一致，不进侧边栏
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT t.menu_name, @mail_menu_id, t.order_num, '', NULL, 1, 0, 'F', '0', '0', t.perms, '#', 1, NOW(), t.remark
FROM (SELECT '查询' AS menu_name, 1 AS order_num, 'report:mailConfig:list' AS perms, '查看邮件配置' AS remark
      UNION ALL SELECT '保存配置', 2, 'report:mailConfig:edit', '保存邮件配置（账号/密码/标题/正文）'
      UNION ALL SELECT '发送测试邮件', 3, 'report:mailConfig:test', '向指定地址发测试邮件') AS t
WHERE @mail_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = @mail_menu_id AND x.perms = t.perms);

-- 授权给超级管理员角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @mail_menu_id OR m.parent_id = @mail_menu_id;
