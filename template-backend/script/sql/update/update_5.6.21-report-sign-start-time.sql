-- 样本流转：报告签名补「签名开始时间」（送签时间）列
-- ============================================================================
-- 背景：样本流转列表要展示「签名件 + 签名开始时间~完成时间」两个时间，
--       但库里只有一个 report_sign_time（列注释 = 签名完成时间，上上签回调写入），
--       送签时间没有落点。
-- 本次口径（用户 2026-10-17 确认）：
--   新增 report_sign_started_at（签名开始时间 = 送签时间，送签时写入）；
--   report_sign_time 保持原语义 = 签名完成时间（上上签回调写入），不改注释。
-- 幂等：列是否存在先查 information_schema，重复执行不报错。
-- ============================================================================
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS p_sign_add_column;

DELIMITER $$
CREATE PROCEDURE p_sign_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(1000))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------------
-- 1. 新增列：签名开始时间（送签时间），放在「签名后 PDF 路径」之后
-- ---------------------------------------------------------------------------
CALL p_sign_add_column('analysis_report', 'report_sign_started_at',
    "`report_sign_started_at` datetime NULL COMMENT '签名开始时间（送签时间）' AFTER `report_signed_file_path`");

-- 存量数据：已有签名件/签名完成时间的行，把开始时间按完成时间回填，避免列表「开始」为空
UPDATE analysis_report
SET report_sign_started_at = report_sign_time
WHERE report_sign_started_at IS NULL AND report_sign_time IS NOT NULL;

DROP PROCEDURE IF EXISTS p_sign_add_column;

-- ---------------------------------------------------------------------------
-- 2. 复核（人工执行）
-- ---------------------------------------------------------------------------
-- SELECT report_id, report_status, report_sign_flow_no, report_sign_started_at, report_sign_time,
--        report_signed_file_path FROM analysis_report ORDER BY report_id;
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_report'
--     AND COLUMN_NAME LIKE 'report_sign%' ORDER BY ORDINAL_POSITION;
