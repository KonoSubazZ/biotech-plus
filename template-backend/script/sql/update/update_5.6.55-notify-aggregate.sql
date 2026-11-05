-- ============================================================================
-- 通知聚合重构（2026-11-03）
--   qc_failed_notify（湿实验 / 生信两类质控不合格通知）→ sample_notify（四类通知台账）
--   ① 表改名，保数据与自增 id
--   ② qc_category → notify_type，取值扩到 delay / sample / wet_lab / bioinfo
--   ③ 状态从中文串改为英文码：notify_status（generated/pending/sent/failed）+ sign_status（none/submitted/signed）
--   ④ 新增：样本名称、研究方案编号/项目编号/参与者编号快照、延期专用两项、
--            四段文件路径 + file_path、处理人/处理时间、发送人/发送时间/收件人/抄送/失败原因
--   ⑤ 新页面「异常处理」菜单 + 提交/发送按钮点位（授权由用户自行配置）
-- 幂等：改名用 information_schema 判存在；加列「不存在才 ALTER」；MODIFY 可重复执行；
--       菜单 NOT EXISTS。MySQL 5.7 兼容（无 CTE / 无 ADD COLUMN IF NOT EXISTS）。
-- 体检：python3 tools/check_db_schema.py
-- ============================================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS p_notify_add_col;
DROP PROCEDURE IF EXISTS p_notify_migrate;

DELIMITER $$

-- 加列工具：列不存在才 ALTER（判据方向：ADD 是「缺失才执行」）
CREATE PROCEDURE p_notify_add_col(IN p_col VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF (SELECT COUNT(*) FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify') = 1
       AND (SELECT COUNT(*) FROM information_schema.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
               AND COLUMN_NAME = p_col) = 0 THEN
        SET @s = CONCAT('ALTER TABLE `sample_notify` ADD COLUMN ', p_ddl);
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;
END$$

CREATE PROCEDURE p_notify_migrate()
BEGIN
    DECLARE v_cnt INT DEFAULT 0;

    -- 0) 改名：旧表存在才改（新表已存在时跳过，避免重复执行报错）
    SELECT COUNT(*) INTO v_cnt FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'qc_failed_notify';
    IF v_cnt = 1 THEN
        SET @s = 'RENAME TABLE `qc_failed_notify` TO `sample_notify`';
        PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
    END IF;

    -- 新装库：表还不存在，交给 business/sample-notify.sql，这里直接返回
    SELECT COUNT(*) INTO v_cnt FROM information_schema.TABLES
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify';
    IF v_cnt = 0 THEN
        SELECT 'sample_notify 不存在，跳过迁移（新装库由 business/sample-notify.sql 建表）' AS msg;
    ELSE
        -- 1) qc_category → notify_type（列存在才改，改完保留原值）
        SELECT COUNT(*) INTO v_cnt FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify' AND COLUMN_NAME = 'qc_category';
        IF v_cnt = 1 THEN
            SET @s = 'ALTER TABLE `sample_notify` CHANGE COLUMN `qc_category` `notify_type` varchar(20) NOT NULL DEFAULT ''bioinfo'' COMMENT ''通知类型（delay 延期通知 / sample 样本不合格 / wet_lab 湿实验不合格 / bioinfo 干实验不合格）''';
            PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
        END IF;
    END IF;
END$$

DELIMITER ;

CALL p_notify_migrate();
DROP PROCEDURE IF EXISTS p_notify_migrate;

-- 2) 加列（过程内部判表/列是否存在；新装库直接跳过）
CALL p_notify_add_col('sample_name', '`sample_name` varchar(100) DEFAULT NULL COMMENT ''样本名称（快照，用于文件命名）'' AFTER `subbarcode`');
CALL p_notify_add_col('sign_status', '`sign_status` varchar(20) NOT NULL DEFAULT ''none'' COMMENT ''签署状态（none 未送签 / submitted 已送签 / signed 已签署）'' AFTER `notify_status`');
CALL p_notify_add_col('study_no', '`study_no` varchar(128) DEFAULT NULL COMMENT ''研究方案编号（快照 = cms_sample.scheme_no）'' AFTER `bioinfo_qc_id`');
CALL p_notify_add_col('project_no', '`project_no` varchar(128) DEFAULT NULL COMMENT ''项目编号（快照 = cms_sample.project_no）'' AFTER `study_no`');
CALL p_notify_add_col('participant_no', '`participant_no` varchar(128) DEFAULT NULL COMMENT ''试验参与者编号（快照 = cms_sample.subject_no）'' AFTER `project_no`');
CALL p_notify_add_col('delay_reason', '`delay_reason` varchar(500) DEFAULT NULL COMMENT ''检测报告延迟原因（delay 专用）'' AFTER `reason`');
CALL p_notify_add_col('expected_report_date', '`expected_report_date` varchar(32) DEFAULT NULL COMMENT ''预计报告出具时间（delay 专用）'' AFTER `delay_reason`');
CALL p_notify_add_col('file_path', '`file_path` varchar(500) DEFAULT NULL COMMENT ''当前交付件路径（生成=PDF；签署完成=签名件；邮件附件取它）'' AFTER `signed_pdf_path`');
CALL p_notify_add_col('handler_by', '`handler_by` bigint DEFAULT NULL COMMENT ''处理人 = 生成该通知的人（sys_user.user_id）'' AFTER `file_path`');
CALL p_notify_add_col('handler_time', '`handler_time` datetime DEFAULT NULL COMMENT ''处理时间 = 生成时间'' AFTER `handler_by`');
CALL p_notify_add_col('sender_by', '`sender_by` bigint DEFAULT NULL COMMENT ''发送人（sys_user.user_id）'' AFTER `handler_time`');
CALL p_notify_add_col('sender_time', '`sender_time` datetime DEFAULT NULL COMMENT ''发送时间'' AFTER `sender_by`');
CALL p_notify_add_col('send_to', '`send_to` varchar(500) DEFAULT NULL COMMENT ''实际收件人（发送时快照，;分隔）'' AFTER `sender_time`');
CALL p_notify_add_col('send_cc', '`send_cc` varchar(500) DEFAULT NULL COMMENT ''实际抄送（发送时快照，;分隔）'' AFTER `send_to`');
CALL p_notify_add_col('send_error', '`send_error` varchar(500) DEFAULT NULL COMMENT ''最近一次发送失败原因（成功时清空）'' AFTER `send_cc`');

DROP PROCEDURE IF EXISTS p_notify_add_col;

-- 3) 数据迁移（表存在才做）
SET @has_table = (SELECT COUNT(*) FROM information_schema.TABLES
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify');

-- 3.1 类型兜底
SET @s = IF(@has_table = 1,
  'UPDATE `sample_notify` SET `notify_type` = ''bioinfo'' WHERE `notify_type` IS NULL OR `notify_type` = ''''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3.2 日期：yyyy-MM-dd → yyyyMMdd（空值用 create_time 补）
SET @s = IF(@has_table = 1,
  'UPDATE `sample_notify` SET `notify_date` = REPLACE(`notify_date`, ''-'', '''') WHERE `notify_date` LIKE ''%-%''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1,
  'UPDATE `sample_notify` SET `notify_date` = DATE_FORMAT(`create_time`, ''%Y%m%d'') WHERE `notify_date` IS NULL OR `notify_date` = ''''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3.3 状态：历史值一律 → generated；签署状态按实际路径推断（比旧中文状态可靠）
SET @s = IF(@has_table = 1,
  'UPDATE `sample_notify` SET `notify_status` = ''generated''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1,
  'UPDATE `sample_notify` SET `sign_status` = CASE
       WHEN `signed_pdf_path` IS NOT NULL AND `signed_pdf_path` <> '''' THEN ''signed''
       WHEN `sign_contract_id` IS NOT NULL AND `sign_contract_id` <> '''' THEN ''submitted''
       ELSE ''none'' END',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3.4 当前交付件 + 处理人/时间
SET @s = IF(@has_table = 1,
  'UPDATE `sample_notify` SET `file_path` = COALESCE(NULLIF(`signed_pdf_path`, ''''), NULLIF(`pdf_path`, ''''), `raw_docx_path`) WHERE `file_path` IS NULL',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1,
  'UPDATE `sample_notify` SET `handler_by` = `create_by`, `handler_time` = `create_time` WHERE `handler_by` IS NULL',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3.5 从 cms_sample 回填三个编号 + 样本名称（cms_sample 有唯一键 tenant_id + subbarcode，不会放大行数）
SET @has_cms = (SELECT COUNT(*) FROM information_schema.TABLES
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'cms_sample');
SET @s = IF(@has_table = 1 AND @has_cms = 1,
  'UPDATE `sample_notify` n
     JOIN `cms_sample` s ON s.`subbarcode` = n.`subbarcode` AND s.`tenant_id` = n.`tenant_id` AND s.`del_flag` = ''0''
      SET n.`sample_name`    = COALESCE(n.`sample_name`, s.`sample_name`),
          n.`study_no`       = COALESCE(n.`study_no`, s.`scheme_no`),
          n.`project_no`     = COALESCE(n.`project_no`, s.`project_no`),
          n.`participant_no` = COALESCE(n.`participant_no`, s.`subject_no`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 4) 列定义对齐（类型/可空/默认值/注释），同时顺手把列顺序收敛成新基线
SET @has_table = (SELECT COUNT(*) FROM information_schema.TABLES
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify');

SET @s = IF(@has_table = 1, 'ALTER TABLE `sample_notify`
    MODIFY COLUMN `notify_no` varchar(64) NOT NULL COMMENT ''通知单号（首次生成：样本编号-yyyyMMddHHmmss；同日重生成不变）'',
    MODIFY COLUMN `notify_type` varchar(20) NOT NULL DEFAULT ''bioinfo'' COMMENT ''通知类型（delay 延期通知 / sample 样本不合格 / wet_lab 湿实验不合格 / bioinfo 干实验不合格）'',
    MODIFY COLUMN `notify_status` varchar(20) NOT NULL DEFAULT ''generated'' COMMENT ''通知状态（generated 已生成待提交 / pending 待处理 / sent 发送成功 / failed 发送失败）'',
    MODIFY COLUMN `sign_status` varchar(20) NOT NULL DEFAULT ''none'' COMMENT ''签署状态（none 未送签 / submitted 已送签 / signed 已签署）'',
    MODIFY COLUMN `notify_date` char(8) NOT NULL COMMENT ''通知日期/出具日期（生成当天 yyyyMMdd）'',
    MODIFY COLUMN `subbarcode` varchar(128) NOT NULL COMMENT ''样本编号（= cms_sample.subbarcode）'',
    MODIFY COLUMN `sample_name` varchar(100) DEFAULT NULL COMMENT ''样本名称（快照，用于文件命名）'',
    MODIFY COLUMN `sample_file_id` bigint DEFAULT NULL COMMENT ''样本信息 id（= cms_sample.id）'',
    MODIFY COLUMN `wet_lab_qc_id` bigint DEFAULT NULL COMMENT ''湿实验质控 id（wet_lab 才有）'',
    MODIFY COLUMN `bioinfo_qc_id` bigint DEFAULT NULL COMMENT ''生信质控 id（bioinfo 才有）'',
    MODIFY COLUMN `study_no` varchar(128) DEFAULT NULL COMMENT ''研究方案编号（快照 = cms_sample.scheme_no）'',
    MODIFY COLUMN `project_no` varchar(128) DEFAULT NULL COMMENT ''项目编号（快照 = cms_sample.project_no）'',
    MODIFY COLUMN `participant_no` varchar(128) DEFAULT NULL COMMENT ''试验参与者编号（快照 = cms_sample.subject_no）'',
    MODIFY COLUMN `delay_reason` varchar(500) DEFAULT NULL COMMENT ''检测报告延迟原因（delay 专用）'',
    MODIFY COLUMN `expected_report_date` varchar(32) DEFAULT NULL COMMENT ''预计报告出具时间（delay 专用）'',
    MODIFY COLUMN `analysis_time` varchar(32) DEFAULT NULL COMMENT ''分析时间（质控记录入库时间）'',
    MODIFY COLUMN `qc_stage` varchar(200) DEFAULT NULL COMMENT ''不合格环节（多选，顿号分隔）'',
    MODIFY COLUMN `terminate_measure` varchar(200) DEFAULT NULL COMMENT ''是否终止检测（多选，顿号分隔）'',
    MODIFY COLUMN `remedy` varchar(300) DEFAULT NULL COMMENT ''建议处理办法（多选，顿号分隔）'',
    MODIFY COLUMN `json_path` varchar(500) DEFAULT NULL COMMENT ''渲染用 JSON 路径（容器内绝对路径）'',
    MODIFY COLUMN `raw_docx_path` varchar(500) DEFAULT NULL COMMENT ''生成的 DOCX 路径'',
    MODIFY COLUMN `pdf_path` varchar(500) DEFAULT NULL COMMENT ''PDF 路径（OnlyOffice 转换产物）'',
    MODIFY COLUMN `signed_pdf_path` varchar(500) DEFAULT NULL COMMENT ''签名后 PDF 路径（上上签回传落盘）'',
    MODIFY COLUMN `file_path` varchar(500) DEFAULT NULL COMMENT ''当前交付件路径（生成=PDF；签署完成=签名件；邮件附件取它）'',
    MODIFY COLUMN `sign_contract_id` varchar(64) DEFAULT NULL COMMENT ''上上签合同号（送签后回填）'',
    MODIFY COLUMN `handler_by` bigint DEFAULT NULL COMMENT ''处理人 = 生成该通知的人（sys_user.user_id）'',
    MODIFY COLUMN `handler_time` datetime DEFAULT NULL COMMENT ''处理时间 = 生成时间'',
    MODIFY COLUMN `sender_by` bigint DEFAULT NULL COMMENT ''发送人（sys_user.user_id）'',
    MODIFY COLUMN `sender_time` datetime DEFAULT NULL COMMENT ''发送时间'',
    MODIFY COLUMN `send_to` varchar(500) DEFAULT NULL COMMENT ''实际收件人（发送时快照，;分隔）'',
    MODIFY COLUMN `send_cc` varchar(500) DEFAULT NULL COMMENT ''实际抄送（发送时快照，;分隔）'',
    MODIFY COLUMN `send_error` varchar(500) DEFAULT NULL COMMENT ''最近一次发送失败原因（成功时清空）''',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 4.1 列顺序收敛（MODIFY ... AFTER；重复执行结果一致）
SET @s = IF(@has_table = 1, 'ALTER TABLE `sample_notify`
    MODIFY COLUMN `notify_no` varchar(64) NOT NULL COMMENT ''通知单号（首次生成：样本编号-yyyyMMddHHmmss；同日重生成不变）'' AFTER `id`,
    MODIFY COLUMN `notify_type` varchar(20) NOT NULL DEFAULT ''bioinfo'' COMMENT ''通知类型（delay 延期通知 / sample 样本不合格 / wet_lab 湿实验不合格 / bioinfo 干实验不合格）'' AFTER `notify_no`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1, 'ALTER TABLE `sample_notify`
    MODIFY COLUMN `notify_status` varchar(20) NOT NULL DEFAULT ''generated'' COMMENT ''通知状态（generated 已生成待提交 / pending 待处理 / sent 发送成功 / failed 发送失败）'' AFTER `notify_type`,
    MODIFY COLUMN `sign_status` varchar(20) NOT NULL DEFAULT ''none'' COMMENT ''签署状态（none 未送签 / submitted 已送签 / signed 已签署）'' AFTER `notify_status`,
    MODIFY COLUMN `notify_date` char(8) NOT NULL COMMENT ''通知日期/出具日期（生成当天 yyyyMMdd）'' AFTER `sign_status`,
    MODIFY COLUMN `subbarcode` varchar(128) NOT NULL COMMENT ''样本编号（= cms_sample.subbarcode）'' AFTER `notify_date`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1, 'ALTER TABLE `sample_notify`
    MODIFY COLUMN `signed_pdf_path` varchar(500) DEFAULT NULL COMMENT ''签名后 PDF 路径（上上签回传落盘）'' AFTER `pdf_path`,
    MODIFY COLUMN `file_path` varchar(500) DEFAULT NULL COMMENT ''当前交付件路径（生成=PDF；签署完成=签名件；邮件附件取它）'' AFTER `signed_pdf_path`,
    MODIFY COLUMN `sign_contract_id` varchar(64) DEFAULT NULL COMMENT ''上上签合同号（送签后回填）'' AFTER `file_path`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1, 'ALTER TABLE `sample_notify`
    MODIFY COLUMN `create_by` bigint DEFAULT NULL COMMENT ''创建者'' AFTER `send_error`,
    MODIFY COLUMN `create_time` datetime DEFAULT NULL COMMENT ''创建时间'' AFTER `create_by`,
    MODIFY COLUMN `update_by` bigint DEFAULT NULL COMMENT ''更新者'' AFTER `create_time`,
    MODIFY COLUMN `update_time` datetime DEFAULT NULL COMMENT ''更新时间'' AFTER `update_by`,
    MODIFY COLUMN `del_flag` char(1) NOT NULL DEFAULT ''0'' COMMENT ''删除标志（0存在 1删除）'' AFTER `update_time`,
    MODIFY COLUMN `tenant_id` varchar(20) NOT NULL DEFAULT ''000000'' COMMENT ''租户编号'' AFTER `del_flag`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 5) 索引：旧索引删掉，新索引（都带 tenant_id 前缀）补齐
SET @s = IF(@has_table = 1 AND (SELECT COUNT(*) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
          AND INDEX_NAME = 'idx_qc_failed_notify_subbarcode') > 0,
  'ALTER TABLE `sample_notify` DROP INDEX `idx_qc_failed_notify_subbarcode`',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1 AND (SELECT COUNT(*) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
          AND INDEX_NAME = 'idx_tenant_id') = 0,
  'ALTER TABLE `sample_notify` ADD KEY `idx_tenant_id` (`tenant_id`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1 AND (SELECT COUNT(*) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
          AND INDEX_NAME = 'idx_sample_notify_subbarcode') = 0,
  'ALTER TABLE `sample_notify` ADD KEY `idx_sample_notify_subbarcode` (`tenant_id`, `subbarcode`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1 AND (SELECT COUNT(*) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
          AND INDEX_NAME = 'idx_sample_notify_day') = 0,
  'ALTER TABLE `sample_notify` ADD KEY `idx_sample_notify_day` (`tenant_id`, `subbarcode`, `notify_type`, `notify_date`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1 AND (SELECT COUNT(*) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
          AND INDEX_NAME = 'idx_sample_notify_status') = 0,
  'ALTER TABLE `sample_notify` ADD KEY `idx_sample_notify_status` (`tenant_id`, `notify_status`, `notify_date`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF(@has_table = 1 AND (SELECT COUNT(*) FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
          AND INDEX_NAME = 'idx_sample_notify_project') = 0,
  'ALTER TABLE `sample_notify` ADD KEY `idx_sample_notify_project` (`tenant_id`, `project_no`)',
  'SELECT 1');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 6) 菜单与按钮（同 business/sample-notify.sql；授权由用户自行配置）
SET @quality_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'sample' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.sample_notify', @quality_id, 4, 'notify', 'sample/notify/index', 1, 0,
    'C', '0', '0', 'qc:notify:list', 'local-icon-email', 1, NOW(), '异常处理（四类通知台账）'
FROM (SELECT 1) AS dummy
WHERE @quality_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @quality_id AND path = 'notify') AS exists_check);

UPDATE sys_menu
SET parent_id = @quality_id, menu_name = 'route.sample_notify', component = 'sample/notify/index',
    path = 'notify', order_num = 4, menu_type = 'C', perms = 'qc:notify:list',
    visible = '0', status = '0', update_by = 1, update_time = NOW()
WHERE parent_id = @quality_id AND path = 'notify';

SET @notify_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @quality_id AND path = 'notify' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT m.menu_name, m.menu_id, m.order_num, '', NULL, 1, 0, 'F', '0', '0', m.perms, '#', 1, NOW(), m.remark
FROM (SELECT @notify_menu_id AS menu_id, '提交' AS menu_name, 1 AS order_num, 'qc:notify:submit' AS perms, '签名完成后提交给发送人' AS remark
      UNION ALL SELECT @notify_menu_id, '发送邮件', 2, 'qc:notify:send', '向客户发送通知邮件') AS m
WHERE m.menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu x
    WHERE x.parent_id = m.menu_id AND x.perms = m.perms);

-- 7) 回读证据
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
 ORDER BY ORDINAL_POSITION;

SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols
  FROM information_schema.STATISTICS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sample_notify'
 GROUP BY INDEX_NAME;

SELECT menu_id, parent_id, menu_name, path, component, perms
  FROM sys_menu
 WHERE menu_id = @notify_menu_id OR parent_id = @notify_menu_id
 ORDER BY parent_id, order_num;

SELECT notify_type, notify_status, sign_status, COUNT(*) AS cnt
  FROM `sample_notify` GROUP BY notify_type, notify_status, sign_status;
