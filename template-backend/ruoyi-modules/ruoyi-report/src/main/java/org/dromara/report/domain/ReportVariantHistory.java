package org.dromara.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.TenantEntity;

import java.util.Date;
import org.dromara.report.service.impl.preview.ReportVariantHistoryWriter;

/**
 * 报告位点检出历史对象 report_variant_history
 * <p>
 * 一行 = 某份报告里的一个「报出」位点；**提交审核**时由 {@code ReportVariantHistoryWriter} 落库
 * （与 history_somatic / history_germline 的冻结同一事务），预览与生成都不写。
 * 同一报告重新提交时按 uk(tenant_id, report_id, source_type, source_id) 覆盖，不追加重复行。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_variant_history")
public class ReportVariantHistory extends TenantEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 报告ID */
    private Long reportId;

    /** 分析批次ID */
    private Long analysisId;

    /** 样本编号 */
    private String subbarcode;

    /** 客户（LIMS 医院名） */
    private String client;

    /** 产品 */
    private String product;

    /** 解读癌种 */
    private String disease;

    /** 来源：SNP_INDEL / CNV / FUSION / CR_ALL */
    private String sourceType;

    /** 源位点明细表主键 */
    private Long sourceId;

    /** 基因 */
    private String gene;

    /** 规范化位点 */
    private String variant;

    /** 丰度（= 预览页「丰度/reads」列：DNA 百分比 / RNA reads / 扩增缺失拷贝数） */
    private String abundance;

    /** 突变类型（VEP ExonicFunc 中文：错义突变/移码突变…） */
    private String variantType;

    /** 原始位点描述 */
    private String oriVariant;

    /** 位点等级：体细胞 I/II/III 类；胚系为临床意义标签 */
    private String level;

    /** 原始用药信息（en7 drugGroups 原样 JSON） */
    private String drugInfo;

    /** 解读人 */
    private String analyzer;

    /** 报告时间（报告生成时间） */
    private Date reportGeneratedAt;

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
