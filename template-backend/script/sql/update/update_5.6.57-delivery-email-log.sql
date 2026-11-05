-- ============================================================================
-- delivery_email_log：交付物邮件发送记录（报告 + 通知共用一张表）
--
-- 口径（2026-11-04 用户确认）：
--   1) 一张表，target_type 区分 report / notify；差异只有 analysis_id / notify_type 两个可空列
--   2) 一行 = 一次发送动作（同一交付物可发多次）
--   3) 报告原 analysis_report_email_log 停止写入，保留历史只读（标记废弃）
--
-- 幂等：CREATE TABLE IF NOT EXISTS，可反复跑。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 biotech < update_5.6.57-delivery-email-log.sql
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `delivery_email_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` char(32) NOT NULL COMMENT '邮件发送追踪号（UUID 去横线）',
  `target_type` varchar(16) NOT NULL COMMENT '交付物类型：report / notify',
  `target_id` bigint(20) NOT NULL COMMENT '报告ID / 通知ID',
  `target_no` varchar(64) DEFAULT NULL COMMENT '报告编号 / 通知单号',
  `analysis_id` bigint(20) DEFAULT NULL COMMENT '分析数据ID（报告专用）',
  `notify_type` varchar(20) DEFAULT NULL COMMENT '通知类型（delay/sample/wet_lab/bioinfo）',
  `subbarcode` varchar(128) DEFAULT NULL COMMENT '样本编号',
  `recipients` varchar(2000) NOT NULL COMMENT '收件人（分号分隔）',
  `recipient_count` bigint(20) NOT NULL DEFAULT '0' COMMENT '收件人数',
  `cc` varchar(2000) DEFAULT NULL COMMENT '抄送（分号分隔）',
  `subject` varchar(500) DEFAULT NULL COMMENT '邮件主题',
  `attachment_name` varchar(255) DEFAULT NULL COMMENT '附件文件名（不含路径）',
  `mail_config_id` bigint(20) DEFAULT NULL COMMENT '发件邮箱配置ID（report_mail_config.id）',
  `from_addr` varchar(200) DEFAULT NULL COMMENT '发件地址',
  `status` varchar(20) NOT NULL COMMENT 'SENDING / SUCCESS / FAILED',
  `provider_message_id` varchar(255) DEFAULT NULL COMMENT '邮件服务商 message-id',
  `failure_reason` varchar(1000) DEFAULT NULL COMMENT '失败原因摘要',
  `triggered_by` varchar(80) NOT NULL COMMENT '触发人',
  `requested_at` datetime NOT NULL COMMENT '发起时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_email_trace` (`tenant_id`,`trace_id`),
  KEY `idx_delivery_email_target` (`target_type`,`target_id`,`requested_at`),
  KEY `idx_delivery_email_status` (`status`,`requested_at`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_delivery_email_subbarcode` (`tenant_id`,`subbarcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交付物邮件发送记录（报告 + 通知）';
