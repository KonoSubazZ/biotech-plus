-- ============================================================================
-- analysis_report 预留「上上签」签名产物列（2026-10-12）
--   为什么：报告交付链路 = 原始 DOCX → 转化 PDF（OnlyOffice）→ 送签 → 回传签名后 PDF → 交付。
--   最终交付物是「签名后 PDF」，需要单独位置与流程状态：
--     report_signed_file_path  签名后 PDF 路径（最终交付；上上签回传后由回调解回）
--     sign_status              签名状态（NOT_SUBMITTED/SIGNING/SIGNED/FAILED；工作流状态，按 §2.1 起具体列名）
--     sign_flow_no             上上签签名流程号（提交时记，回调对账用）
--     sign_time                签名完成时间
--   本次只落列与状态位，不做真实对接（上上签未实现）。
--
-- 幂等：MySQL 5.7 无 ADD COLUMN IF NOT EXISTS，先查 information_schema 再 ALTER。
-- 配套：template-backend/script/sql/business/report-interpretation.sql（新装库基线，同口径）
-- ============================================================================

SET NAMES utf8mb4;

SET @ddl = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `analysis_report`
        ADD COLUMN `report_signed_file_path` VARCHAR(1000) DEFAULT NULL COMMENT ''签名后PDF报告路径（上上签回传，最终交付）'',
        ADD COLUMN `sign_status`             VARCHAR(20)   DEFAULT NULL COMMENT ''签名状态（NOT_SUBMITTED/SIGNING/SIGNED/FAILED）'',
        ADD COLUMN `sign_flow_no`            VARCHAR(80)   DEFAULT NULL COMMENT ''上上签签名流程号'',
        ADD COLUMN `sign_time`               DATETIME      DEFAULT NULL COMMENT ''签名完成时间''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_report' AND COLUMN_NAME = 'report_signed_file_path');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 自检
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT
  FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_report'
   AND COLUMN_NAME IN ('report_signed_file_path','sign_status','sign_flow_no','sign_time');
