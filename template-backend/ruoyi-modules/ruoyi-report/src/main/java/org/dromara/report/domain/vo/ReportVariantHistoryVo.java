package org.dromara.report.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.report.domain.ReportVariantHistory;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 报告位点检出历史 视图对象（「历史检出」列表页直接展示）
 * <p>
 * 用药信息：库里只存一列原始 JSON（{@code drug_info}），接口按 en7 口径吐 8 个平铺字段
 * （{@link #drugsA}…{@link #resistantDrugsD}），解析在 {@code ReportVariantHistoryServiceImpl} 里做，
 * 前端不接触原始 JSON。空分组 = null。
 *
 * @author <你的名字>
 */
@Data
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = ReportVariantHistory.class)
public class ReportVariantHistoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 报告ID */
    private Long reportId;

    /** 样本编号 */
    private String subbarcode;

    /** 客户 */
    private String client;

    /** 项目编号（替代 client 做归属展示） */
    private String projectNo;

    /** 产品 */
    private String product;

    /** 解读癌种 */
    private String disease;

    /** 来源：SNP_INDEL / CNV / FUSION / CR_ALL */
    private String sourceType;

    /** 基因 */
    private String gene;

    /** 规范化位点 */
    private String variant;

    /** 丰度（= 预览页「丰度/reads」列；无口径为空） */
    private String abundance;

    /** 突变类型（VEP ExonicFunc 中文：错义突变/移码突变…；无则空） */
    private String variantType;

    /** 原始位点描述 */
    private String oriVariant;

    /** 位点等级（胚系为临床意义标签） */
    private String level;

    /**
     * 原始用药信息（en7 drugGroups 原样 JSON）。
     * <p>
     * **不序列化**：库里原样留着（无损、可追溯），接口只吐下面 8 个解析好的分组字段。
     */
    @JsonIgnore
    private String drugInfo;

    /** 用药信息 · 获益A级（药名串，`;` 分隔；无则 null） */
    private String drugsA;

    /** 用药信息 · 获益B级（药名串，`;` 分隔；无则 null） */
    private String drugsB;

    /** 用药信息 · 获益C级（药名串，`;` 分隔；无则 null） */
    private String drugsC;

    /** 用药信息 · 获益D级（药名串，`;` 分隔；无则 null） */
    private String drugsD;

    /** 用药信息 · 耐药A级（药名串，`;` 分隔；无则 null） */
    private String resistantDrugsA;

    /** 用药信息 · 耐药B级（药名串，`;` 分隔；无则 null） */
    private String resistantDrugsB;

    /** 用药信息 · 耐药C级（药名串，`;` 分隔；无则 null） */
    private String resistantDrugsC;

    /** 用药信息 · 耐药D级（药名串，`;` 分隔；无则 null） */
    private String resistantDrugsD;

    /** 解读人 */
    private String analyzer;

    /** 报告时间（报告生成时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date reportGeneratedAt;
}
