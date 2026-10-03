package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 解读页 Tab③ 筛选位点：位点行
 * <p>
 * 四类位点分散在四张表（file_Somatic_SNV_Indel / file_CNV / file_Fusion / file_CR_ALL），
 * 字段语义各不相同，所以这里用一张「并集」VO：只填当前类型有的字段，其余为 null，
 * 前端按 sourceType 决定显示哪几列（避免为四张表各写一套分页接口/页面）。
 * <p>
 * 字段名对应各表列见 {@code InterpretationMapper.xml} 的四个分支。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationVariantVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 位点主键（各明细表的 id） */
    private Long sourceId;

    /** 位点类型：SNP_INDEL / CNV / FUSION / CR_ALL */
    private String sourceType;

    /** 来源文件ID（data_file_status.file_id） */
    private Long fileId;

    // ---------------- 公共 ----------------
    /** 基因 */
    private String gene;
    /** 突变（蛋白改变，如 V559D / 拷贝数 / 融合名） */
    private String variant;
    /** 原始变异描述（转录本 外显子 cHGVS pHGVS） */
    private String oriVariant;
    /** 是否纳入报告：0 否 / 1 是 */
    private Integer isReported;
    /** 过滤理由（不报出时填写） */
    private String filteredRationale;
    /** 最近一次「入报告」操作时间 */
    private Date reviewedAt;

    // ---------------- SNP/Indel ----------------
    /** 转录本 */
    private String transcript;
    /** 外显子 */
    private String exon;
    /** 突变类型（如 nonsynonymous SNV） */
    private String mutationType;
    /** 染色体 */
    private String chromosome;
    /** 起始位置 */
    private String position;
    /** 合子状态（SNP/Indel 的 hom_het、CR_ALL 的 zygosity） */
    private String homHet;
    /** 突变丰度（%） */
    private String mutFreq;
    /** 突变深度 */
    private String mutDepth;
    /** 总深度 */
    private String totalDepth;

    // ---------------- CNV ----------------
    /** 拷贝数 */
    private String copyNum;

    // ---------------- Fusion ----------------
    /** 融合 5' 基因 */
    private String gene1;
    /** 融合 3' 基因 */
    private String gene2;
    /** DNA/RNA */
    private String tag;
    /** 融合 reads 数 */
    private String fusionReads;
    /** 检测结果（Fusion 表的 check_result） */
    private String checkResult;

    // ---------------- CR_ALL（胚系） ----------------
    /** cHGVS */
    private String chgvs;
    /** pHGVS */
    private String phgvs;
    /** 知识库临床意义（CLNSIG，原始文本） */
    private String clinicalSignificance;
}
