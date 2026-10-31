package org.dromara.qc.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 样本质控不合格通知 表单对象（= 通知单弹窗里的字段，与文档一一对应）
 * <p>
 * 勾选项（样本质控环节 / 是否终止检测 / 建议处理办法）传 boolean：
 * 页面用多选框，落库时服务端转成中文名（顿号分隔），渲染时模板用 {@code {{ box(flag) }}} 输出 ☑/□。
 *
 * @author <你的名字>
 */
@Data
public class QcFailedNotifyBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 样本编号（必填；后面所有回填都按它查 cms_sample） */
    @NotBlank(message = "样本编号不能为空")
    @Size(max = 100, message = "样本编号长度不能超过 100")
    private String subbarcode;

    /**
     * 质控类型（wet_lab 湿实验 / bioinfo 生信）—— 在哪个质控页点「处理」的；
     * 为空按 bioinfo 处理（老调用方兼容），落库进 qc_failed_notify.qc_category
     */
    @Size(max = 20, message = "质控类型长度不能超过 20")
    private String qcCategory;

    // ── 通知单表头 ────────────────────────────────────────────────
    /** 研究方案编号 */
    @Size(max = 200, message = "研究方案编号过长")
    private String studyNo;

    /** 诺禾项目编号 */
    @Size(max = 200, message = "诺禾项目编号过长")
    private String projectNo;

    /** 试验参与者编号 */
    @Size(max = 200, message = "试验参与者编号过长")
    private String participantNo;

    /** 研究中心名称 */
    @Size(max = 200, message = "研究中心名称过长")
    private String center;

    /** 样本接收日期 */
    @Size(max = 64, message = "样本接收日期过长")
    private String receiveDate;

    /** 样本类型 */
    @Size(max = 100, message = "样本类型过长")
    private String sampleType;

    /** 样本数量 */
    @Size(max = 64, message = "样本数量过长")
    private String sampleCount;

    /** 检测产品 */
    @Size(max = 256, message = "检测产品过长")
    private String product;

    // ── 样本质控环节（多选） ───────────────────────────────────────
    /** 样本接收 */
    private Boolean stageReceive;

    /** 病理质控 */
    private Boolean stagePathology;

    /** 核酸质控 */
    private Boolean stageNucleic;

    /** 文库质控 */
    private Boolean stageLibrary;

    /** 测序质控 */
    private Boolean stageSequencing;

    // ── 不合格原因与处置 ──────────────────────────────────────────
    /** 不合格原因 */
    @Size(max = 500, message = "不合格原因长度不能超过 500")
    private String reason;

    /** 是否终止检测-是 */
    private Boolean terminateYes;

    /** 是否终止检测-暂停相关检测，待申办方确认后处理 */
    private Boolean terminatePause;

    /** 建议处理办法-重新采集符合要求的样本重新寄送 */
    private Boolean remedyResample;

    /** 建议处理办法-更换肿瘤含量较高的蜡块重新切片后寄送 */
    private Boolean remedyWaxBlock;

    /** 建议处理办法-选取组织面积较大或较多的切片重新寄送 */
    private Boolean remedyMoreSlices;

    /** 建议处理办法-重新送样 */
    private Boolean remedyResend;

    /** 建议处理办法-其他 */
    private Boolean remedyOther;

    /** 建议处理办法-其他（文本） */
    @Size(max = 500, message = "「其他」说明过长")
    private String remedyOtherText;

    // ── 反馈与复核 ────────────────────────────────────────────────
    /** 反馈人 */
    @Size(max = 100, message = "反馈人过长")
    private String reporter;

    /** 反馈时间 */
    @Size(max = 64, message = "反馈时间过长")
    private String reportTime;

    /** 复核人 */
    @Size(max = 100, message = "复核人过长")
    private String reviewer;

    /** 复核时间 */
    @Size(max = 64, message = "复核时间过长")
    private String reviewTime;
}
