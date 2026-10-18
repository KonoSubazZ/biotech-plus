package org.dromara.audit.domain.dto;

import lombok.Getter;
import java.util.Arrays;
import java.util.List;

/** 表名和可定位列只能来自此服务端白名单。 */
@Getter
public enum AuditSource {
    SAMPLE("sample_file", "id", "subbarcode"),
    WET_QC("wet_lab_qc", "id", "subbarcode"),
    BIO_QC("bioinfo_qc", "id", "subbarcode"),
    ANALYSIS("analysis_data", "analysis_id", "subbarcode"),
    REPORT("analysis_report", "report_id", "analysis_id", "subbarcode", "report_sign_flow_no"),
    FILE("data_file_status", "file_id", "analysis_id", "subbarcode"),
    SNV("file_Somatic_SNV_Indel", "id", "file_id"),
    CNV("file_CNV", "id", "file_id"),
    FUSION("file_Fusion", "id", "file_id"),
    GERMLINE("file_CR_ALL", "id", "file_id"),
    SOMATIC_HISTORY("history_somatic", "id", "match_key", "source_analysis_id"),
    GERMLINE_HISTORY("history_germline", "id", "match_key", "source_analysis_id"),
    REPORT_HISTORY("report_variant_history", "id", "report_id", "analysis_id"),
    REUSE_HISTORY("history_reuse_log", "id", "analysis_id"),
    SEQUENCE("report_version_sequence", "id", "report_id"),
    EMAIL("analysis_report_email_log", "id", "report_id", "analysis_id"),
    USER("sys_user", "user_id", "user_name"),
    USER_ROLE("sys_user_role", "user_id,role_id", "user_id", "role_id"),
    ROLE("sys_role", "role_id", "role_key"),
    ROLE_MENU("sys_role_menu", "role_id,menu_id", "role_id");

    private final String tableName;
    private final List<String> primaryKeys;
    private final List<String> selectors;

    AuditSource(String tableName, String primaryKeys, String... selectors) {
        this.tableName = tableName;
        this.primaryKeys = Arrays.asList(primaryKeys.split(","));
        this.selectors = Arrays.asList(selectors);
    }
}
