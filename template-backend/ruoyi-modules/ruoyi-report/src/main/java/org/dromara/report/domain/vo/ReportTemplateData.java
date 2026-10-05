package org.dromara.report.domain.vo;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告 JSON 总实体类（对齐设计书 §7.6 契约与参考工程 ReportTemplateData）。
 * <p>
 * 分两层，这是「新增模板字段不动公共类」的关键：
 * <ol>
 *   <li><b>公共字段</b>：跨模板语义一致的部分（reportInfo / sampleInfo / 位点两节 / 开关 / warnings），
 *       由 CommonReportModuleAssembler 无条件写入，直接定义成本类的强类型字段。</li>
 *   <li><b>模板专属字段</b>：不写字段，走 {@link #personalizedModules} + {@link JsonAnyGetter}，
 *       由各个 Handler 用 {@code putPersonalizedModule("xxx", 值)} 挂到<b>顶层</b>
 *       （如 shengyuSomaticVariants / shengyuGermlineVariants / qualityControl），
 *       因此新增模板 = 新增/组合 Handler + 在 report_template.module_code 里配编码，公共类一个字不改。</li>
 * </ol>
 * 确定性要求：同输入必须产生相同 JSON —— 不写当前时间、不写随机值；
 * 缺值保持 null（只有明确要展示的位才由 Handler 补 "/"）。
 *
 * @author <你的名字>
 */
@Data
public class ReportTemplateData implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** JSON 协议版本（DOCX 渲染器按它识别结构；纯新增字段不升版本） */
    private String schemaVersion = "1.2";

    /** 模板编码 */
    private String templateCode;

    /** 模板版本 */
    private String templateVersion;

    /** 分析批次ID */
    private Long analysisId;

    /** 报告ID */
    private Long reportId;

    /** 模板主键（登记 analysis_report.template_id 用；不进 JSON） */
    @JsonIgnore
    private Long templateId;

    /** 报告基础信息（公共） */
    private ReportInfo reportInfo;

    /** 是否展示「组织 + 全血」样本字段（公共开关，模板用来分支） */
    private boolean showTissueBloodSample;

    /** 是否展示「仅血液」样本字段（公共开关） */
    private boolean showBloodOnlySample;

    /** 样本信息（公共，LIMS 映射） */
    private SampleInfo sampleInfo;

    /** 体细胞变异解析（公共完整预览：summary + items） */
    private PreviewSectionVo somaticVariants;

    /** 肿瘤遗传风险 / 胚系变异（公共完整预览：summary + items） */
    private PreviewSectionVo germlineVariants;

    /** 非关键缺失等告警；不阻断预览 */
    private List<String> warnings;

    /** 模板专属字段（顶层动态字段容器；不序列化成 personalizedModules 这个键） */
    private final Map<String, Object> personalizedModules = new LinkedHashMap<>();

    /** 序列化时把 personalizedModules 的每个键直接展开成顶层字段 */
    @JsonAnyGetter
    public Map<String, Object> personalizedModules() {
        return personalizedModules;
    }

    /** 供 Java 侧读取（不参与序列化，避免出现重复的 personalizedModules 键） */
    @JsonIgnore
    public Map<String, Object> personalizedModuleMap() {
        return personalizedModules;
    }

    /** Handler 用：挂一个模板专属顶层字段 */
    public void putPersonalizedModule(String field, Object value) {
        personalizedModules.put(field, value);
    }

    /** Handler 用：读一个模板专属顶层字段 */
    public Object personalizedModule(String field) {
        return personalizedModules.get(field);
    }

    /** 报告基础信息（公共字段，仅这两个键） */
    @Data
    public static class ReportInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 报告日期（yyyy-MM-dd，取分析日期，不取当前时间） */
        private String reportDate;

        /** 标本类型（tissue / blood；沿用参考工程的拼写 specimentType） */
        private String specimentType;

        /** 报告名（report_template.report_name 渲染结果；供 DOCX 封面/页眉引用，与落盘文件名同源） */
        private String reportName;
    }

    /** 样本信息（公共字段；字段名与设计书 §7.6 / 参考工程逐字一致） */
    @Data
    public static class SampleInfo implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 研究中心（送检单位） */
        private String researchCenterName;

        /** 受试者编号 */
        private String participantNumber;

        /** 性别（女 / 男） */
        private String gender;

        /** 出生年份（4 位） */
        private String birthYear;

        /** 疾病（解读癌种） */
        private String disease;

        /** 访视周期（本仓 LIMS 无此字段，固定 null） */
        private String visitCycle;

        /** 样本编号 */
        private String sampleCode;

        /** 样本类型（组织+全血 / 组织 / 全血） */
        private String sampleType;

        /** 组织采样日期 */
        private String tissueCollectionDate;

        /** 切片日期（本仓 LIMS 无此字段，固定 null） */
        private String sectionDate;

        /** 血液采样日期（本仓 LIMS 无此字段，固定 null） */
        private String bloodCollectionDate;

        /** 收样日期 */
        private String receivedDate;
    }

    /** 圣域个性化：体细胞一行 */
    @Data
    public static class SomaticVariant implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 基因 */
        private String gene;

        /** 突变类型（SNV/Indel、CNV、Fusion；无位点补 "/"） */
        private String mutationType;

        /** 结果（规范化位点；无位点补 "/"） */
        private String result;

        /** 丰度或拷贝数（无位点补 "/"） */
        private String abundanceOrCopyNumber;

        /** 位点分级（无位点补 "/"） */
        private String classification;
    }

    /** 圣域个性化：胚系一行 */
    @Data
    public static class GermlineVariant implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 基因 */
        private String gene;

        /** 突变类型 */
        private String mutationType;

        /** 结果 */
        private String result;

        /** 合子状态 */
        private String zygosity;

        /** 临床意义（五级标签：致病/可能致病/未知临床意义/可能良性/良性） */
        private String classification;
    }

    /**
     * 质控（圣域个性化；按用户口径<b>只带值</b>）。
     * <p>
     * 阈值与「合格/不合格」判定文案<b>静态写在 DOCX 模板里</b>，不进 JSON —— 所以这里没有
     * standard / status / assessment 字段。
     */
    @Data
    public static class QualityControl implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        /** 标本类型（组织 / 血液） */
        private String specimenType;

        /** 样本质控指标（key = 指标英文驼峰名，value = 值；阈值在模板里） */
        private Map<String, String> sampleQc;

        /** 对照质控指标（同上） */
        private Map<String, String> controlQc;
    }
}
