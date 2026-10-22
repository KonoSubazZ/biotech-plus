package org.dromara.report.domain.dto;

import lombok.Data;

import java.util.List;
import org.dromara.report.service.impl.NkbDiseaseScopeResolver;

/**
 * 报告预览上下文（一次性把公共查询做完，避免每个位点重复查库）
 * <p>
 * 用 @Data 类而不是 record：字段有 13 个，record 的构造参数会被可读性规则判为「参数过多」。
 *
 * @author <你的名字>
 */
@Data
public class PreviewContext {

    private Long analysisId;

    private Long reportId;

    private String templateCode;

    private String templateVersion;

    private String moduleCode;

    private String disease;

    private Long diseaseId;

    private List<Long> diseaseIds;

    /** 癌种范围（本癌种 + 祖先 + 子孙，已按性别/瘤种剔除） */
    private NkbDiseaseScopeResolver.DiseaseScope diseaseScope;

    private String gender;

    private String customer;

    /** 项目编号（analysis_report.project_no；历史表与报告 JSON 的公共归属字段） */
    private String projectNo;

    private String projectCode;

    private List<String> productGenes;

    private String specimenType;

}
