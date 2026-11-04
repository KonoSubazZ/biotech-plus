package org.dromara.qc.utils;

import java.util.Set;

/**
 * 样本通知的类型 / 状态口径（代码与页面共用一份，避免各处写字符串）。
 * <p>
 * 状态机：generated 已生成 →（送签/签署）→ pending 待处理（提交后）→ sent 发送成功 / failed 发送失败。
 *
 * @author <你的名字>
 */
public final class NotifyConstants {

    private NotifyConstants() {
    }

    /** 通知类型 */
    public static final class Type {
        /** 延期通知 */
        public static final String DELAY = "delay";
        /** 样本不合格 */
        public static final String SAMPLE = "sample";
        /** 湿实验不合格 */
        public static final String WET_LAB = "wet_lab";
        /** 干实验不合格（生信质控） */
        public static final String BIOINFO = "bioinfo";

        public static final Set<String> ALL = Set.of(DELAY, SAMPLE, WET_LAB, BIOINFO);

        private Type() {
        }

        /** 类型 → 中文（页面标签 / 日志） */
        public static String label(String type) {
            return switch (type == null ? "" : type) {
                case DELAY -> "延期通知";
                case SAMPLE -> "样本不合格";
                case WET_LAB -> "湿实验不合格";
                case BIOINFO -> "干实验不合格";
                default -> "未知通知";
            };
        }

        /** 类型 → 单据名（文件名 / 邮件正文用） */
        public static String docTypeName(String type) {
            return DELAY.equals(type) ? "检测报告延迟通知单" : "样本质控不合格通知单";
        }

        /** 类型 → 邮件标题前缀 */
        public static String mailSubjectPrefix(String type) {
            return DELAY.equals(type) ? "【检测报告延迟通知】" : "【样本质控不合格通知】";
        }

        public static boolean isDelay(String type) {
            return DELAY.equals(type);
        }

        public static boolean isValid(String type) {
            return type != null && ALL.contains(type);
        }
    }

    /** 通知状态 */
    public static final class Status {
        /** 已生成（待提交） */
        public static final String GENERATED = "generated";
        /** 待处理（已提交，等着发邮件） */
        public static final String PENDING = "pending";
        /** 发送成功 */
        public static final String SENT = "sent";
        /** 发送失败（可重发） */
        public static final String FAILED = "failed";

        private Status() {
        }

        public static String label(String status) {
            return switch (status == null ? "" : status) {
                case GENERATED -> "已生成（待提交）";
                case PENDING -> "待处理";
                case SENT -> "发送成功";
                case FAILED -> "发送失败";
                default -> "未知状态";
            };
        }
    }

    /** 签署状态 */
    public static final class Sign {
        /** 未送签 */
        public static final String NONE = "none";
        /** 已送签（等对方签） */
        public static final String SUBMITTED = "submitted";
        /** 已签署（签名件已落盘） */
        public static final String SIGNED = "signed";

        private Sign() {
        }
    }

    /**
     * 通知邮件默认文案（占位符用 {@code {{key}}}，由服务端按记录渲染；页面可改后再发）。
     */
    public static final class Mail {
        /** 标题模板（前缀按类型取 {@link Type#mailSubjectPrefix(String)}） */
        public static final String SUBJECT =
            "{{prefix}}{{subbarcode}}-{{sampleName}}（{{participantNo}}）-{{projectNo}}-{{notifyDate}}";

        /** 延期通知正文 */
        public static final String BODY_DELAY = """
            老师，您好！

            您的送检订单{{studyNo}}（研究方案编号）项目 {{participantNo}}（试验参与者编号）样本，\
            因（{{delayReason}}）原因，检测报告延迟至（{{expectedReportDate}}）出具，请查看附件检测报告延迟通知单。

            如您对报告有任何问题请联系诺禾实验室项目负责人。或发邮件至lab.oncology.ct@novogene.com。\
            本邮件为系统自动触发邮件，请勿直接回复。祝您工作顺利!""";

        /** 三类不合格通知正文（同一套） */
        public static final String BODY_QC = """
            老师，您好！

            您的送检订单{{studyNo}}（研究方案编号）项目 {{participantNo}}（试验参与者编号）\
            {{sampleType}}（样本类型）样本，因（{{reason}}）原因，样本质控不合格，请查看附件样本质控不合格通知单。

            如对报告有任何问题请联系诺禾实验室项目负责人。或发邮件至lab.oncology.ct@novogene.com。\
            本邮件为系统自动触发邮件，请勿直接回复。祝您工作顺利!""";

        private Mail() {
        }
    }
}
