package org.dromara.report.utils;

import java.util.Map;

/**
 * 报告状态机（{@code analysis_report.report_status}，int 码）。
 * <p>
 * 状态是「线性 + 若干失败旁支」：没有前一个状态就没有后一个状态的数据。
 * <pre>
 * 20 生信看点 → 21 生信审核 → 30 报告待解读 → 31 报告解读中 →(32 生成成功 / 33 生成失败)
 *   → 40 报告待审核 → 41 报告审核中 →(43 审核成功 → 50 签名中 → 53 签名成功)
 *                                  \-(42 审核失败 → 回退 31)
 *   → 60 报告待发送 →(62 发送成功 / 61 发送失败)
 * </pre>
 * 43 / 53 是**瞬时态**：43 写审核人后立刻自动送签转 50；53 收到签名件后自动转 60。
 * <p>
 * 这里只放状态码与文案（跨层共用），**转移合法性判断留在各 service 里**，避免常量类承载业务判定。
 *
 * @author <你的名字>
 */
public final class ReportStatus {

    /** 20 生信看点：报告刚创建/进入看点 */
    public static final int BIOINFO_VIEW = 20;

    /** 21 生信审核：看点完成，待生信复核 */
    public static final int BIOINFO_REVIEW = 21;

    /** 30 报告待解读：生信复核通过，待解读员认领 */
    public static final int PENDING_INTERPRET = 30;

    /** 31 报告解读中：已被解读员认领（也是审核失败回退后的落点） */
    public static final int INTERPRETING = 31;

    /** 32 报告生成成功 */
    public static final int GENERATED = 32;

    /** 33 报告生成失败 */
    public static final int GENERATE_FAILED = 33;

    /** 40 报告待审核：解读已提交，待复核员认领 */
    public static final int PENDING_REVIEW = 40;

    /** 41 报告审核中：复核员已认领 */
    public static final int REVIEWING = 41;

    /** 42 报告审核失败（回退时记进 report_revert_from，落点状态是 31） */
    public static final int REVIEW_FAILED = 42;

    /** 43 报告审核成功（瞬时态，随后自动送签转 50） */
    public static final int REVIEW_APPROVED = 43;

    /** 50 报告签名中：已送上上签，等回调/手动刷新 */
    public static final int SIGNING = 50;

    /** 52 报告签名失败：可重新送签 */
    public static final int SIGN_FAILED = 52;

    /** 53 报告签名成功（瞬时态，随后自动转 60） */
    public static final int SIGNED = 53;

    /** 60 报告待发送 */
    public static final int PENDING_SEND = 60;

    /** 61 报告发送失败：可重发 */
    public static final int SEND_FAILED = 61;

    /** 62 报告发送成功＝完成报告（终态，也是「可以再新建报告」的判据） */
    public static final int COMPLETED = 62;

    /** 状态码 → 中文（页面标签/报错文案共用一份，别在别处再抄一遍） */
    private static final Map<Integer, String> LABELS = Map.ofEntries(
        Map.entry(BIOINFO_VIEW, "生信看点"),
        Map.entry(BIOINFO_REVIEW, "生信审核"),
        Map.entry(PENDING_INTERPRET, "报告待解读"),
        Map.entry(INTERPRETING, "报告解读中"),
        Map.entry(GENERATED, "报告生成成功"),
        Map.entry(GENERATE_FAILED, "报告生成失败"),
        Map.entry(PENDING_REVIEW, "报告待审核"),
        Map.entry(REVIEWING, "报告审核中"),
        Map.entry(REVIEW_FAILED, "报告审核失败"),
        Map.entry(REVIEW_APPROVED, "报告审核成功"),
        Map.entry(SIGNING, "报告签名中"),
        Map.entry(SIGN_FAILED, "报告签名失败"),
        Map.entry(SIGNED, "报告签名成功"),
        Map.entry(PENDING_SEND, "报告待发送"),
        Map.entry(SEND_FAILED, "报告发送失败"),
        Map.entry(COMPLETED, "报告发送成功")
    );

    /** 未知状态码原样显示，不要静默当成功 */
    public static String label(Integer code) {
        if (code == null) {
            return "未知";
        }
        return LABELS.getOrDefault(code, String.valueOf(code));
    }

    private ReportStatus() {
    }
}
