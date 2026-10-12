package org.dromara.report.domain.vo;

/**
 * 报告签名状态（上上签）。
 * <p>
 * 这是<b>工作流状态</b>，不是启用/停用开关，按 ai-rules/04-db-schema.md §2.1 起具体常量名、
 * 不套 active/inactive；落库到 {@code analysis_report.sign_status}。
 *
 * @author <你的名字>
 */
public final class ReportSignStatus {

    /** 未提交（默认：没走签名流程） */
    public static final String NOT_SUBMITTED = "NOT_SUBMITTED";

    /** 已提交，等签名/回调 */
    public static final String SIGNING = "SIGNING";

    /** 已签名（签名后 PDF 已回传落 signed/） */
    public static final String SIGNED = "SIGNED";

    /** 签名失败 */
    public static final String FAILED = "FAILED";

    private ReportSignStatus() {
    }
}
