package org.dromara.qc.domain.vo;

import lombok.Data;

/**
 * 不合格通知「处理」弹窗的预填数据
 * <p>
 * 表头字段按 subbarcode 从 sample_file 回填；若该样本已经生成过通知，则用**最新一条记录**的值覆盖
 * （lastGenerated* 带上一次生成的单据信息，前端据此提示「已生成」并可直接改后重新生成）。
 *
 * @author <你的名字>
 */
@Data
public class QcFailedNotifyPrefillVo {

    // ── 关联关系（表头信息） ───────────────────────────────────────
    /** cms_sample.id（一定有） */
    private Long sampleFileId;

    /** 生信质控 id（湿实验合格后才可能有） */
    private Long bioinfoQcId;

    /** 湿实验质控 id（wet_lab_qc.id；在湿实验页点处理时带回） */
    private Long wetLabQcId;

    /** 质控类型（wet_lab 湿实验 / bioinfo 生信）—— 这条通知是在哪个质控页点处理产生的 */
    private String qcCategory;

    /** 分析时间（质控记录入库时间，自动带出） */
    private String analysisTime;

    /** 客户（= 药企名） */
    private String customerName;

    /** 上次生成的记录 id（下载用） */
    private Long lastNotifyId;

    /** 通知单号（上次生成时的） */
    private String notifyNo;

    /** 通知状态：已生成（文档）/ 已发送（邮件） */
    private String notifyStatus;

    /** 上次生成时间 */
    private String lastGeneratedAt;

    /** 上次生成的 DOCX 文件名 */
    private String lastDocxName;

    /** 上次生成的 PDF 路径；有值表示该记录已转出 PDF（下载优先给 PDF） */
    private String lastPdfName;

    /** 上上签合同号（送签后才有；前端据此启用「刷新签署状态」） */
    private String signContractId;

    /** 签名后 PDF 路径（签署完成并拉取后才有；有值表示可直接下载签名件） */
    private String signedPdfPath;

    /** 上次落库的不合格环节（中文名，顿号分隔；用于反解勾选位） */
    private String qcStage;

    /** 上次落库的是否终止检测（中文名） */
    private String terminateMeasure;

    /** 上次落库的建议处理办法（中文名） */
    private String remedy;

    /** 是否已经生成过 */
    private Boolean generated;

    // ── 表单字段（与文档一一对应） ─────────────────────────────────
    /** 样本编号 */
    private String subbarcode;

    /** 研究方案编号 */
    private String studyNo;

    /** 诺禾项目编号 */
    private String projectNo;

    /** 试验参与者编号 */
    private String participantNo;

    /** 研究中心名称 */
    private String center;

    /** 样本接收日期 */
    private String receiveDate;

    /** 样本类型 */
    private String sampleType;

    /** 样本数量 */
    private String sampleCount;

    /** 检测产品 */
    private String product;

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

    /** 不合格原因 */
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
    private String remedyOtherText;

    /** 反馈人 */
    private String reporter;

    /** 反馈时间 */
    private String reportTime;

    /** 复核人 */
    private String reviewer;

    /** 复核时间 */
    private String reviewTime;
}
