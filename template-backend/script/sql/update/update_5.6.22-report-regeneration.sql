-- 报告业务版本与重生成。现有测试记录不回填版本、不删除文件。
-- 回滚：回退应用后保留新增列和序列表；不得删除已经发送的业务版本登记。
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS report_version_sequence (
    id bigint NOT NULL AUTO_INCREMENT COMMENT '版本序列ID',
    tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
    client varchar(80) NOT NULL COMMENT '模板客户编码快照',
    subbarcode varchar(80) NOT NULL COMMENT '样本编号',
    template_id bigint NOT NULL COMMENT '模板ID',
    base_name varchar(500) DEFAULT NULL COMMENT '首次生成确定的名称主体，后续版本沿用',
    sent_version int NOT NULL DEFAULT 0 COMMENT '最后成功发送的业务版本',
    active_report_id bigint DEFAULT NULL COMMENT '当前占号报告ID',
    create_by bigint DEFAULT NULL COMMENT '创建者',
    create_time datetime DEFAULT NULL COMMENT '创建时间',
    update_by bigint DEFAULT NULL COMMENT '更新者',
    update_time datetime DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_version_sequence (tenant_id, client, subbarcode, template_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='报告版本序列';

DROP PROCEDURE IF EXISTS p_report_revision_column;
DELIMITER $$
CREATE PROCEDURE p_report_revision_column(IN column_name_value varchar(64), IN definition_value text)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'analysis_report' AND column_name = column_name_value) THEN
        SET @report_revision_ddl = CONCAT('ALTER TABLE analysis_report ADD COLUMN ',
            column_name_value, ' ', definition_value);
        PREPARE report_revision_stmt FROM @report_revision_ddl;
        EXECUTE report_revision_stmt;
        DEALLOCATE PREPARE report_revision_stmt;
    END IF;
END$$
DELIMITER ;
CALL p_report_revision_column('report_version', 'int NULL COMMENT ''业务版本，首次生成时预留''');
CALL p_report_revision_column('report_sequence_id', 'bigint NULL COMMENT ''业务版本序列ID''');
CALL p_report_revision_column('report_generation_no', 'int NOT NULL DEFAULT 0 COMMENT ''内部生成轮次''');
CALL p_report_revision_column('report_reviewer_id', 'bigint NULL COMMENT ''重生成后的定向复审人''');
CALL p_report_revision_column('source_report_id', 'bigint NULL COMMENT ''重新解读来源报告ID''');
DROP PROCEDURE p_report_revision_column;
