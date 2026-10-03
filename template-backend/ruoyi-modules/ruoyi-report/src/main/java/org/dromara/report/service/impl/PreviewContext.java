package org.dromara.report.service.impl;

import lombok.Data;

import java.util.List;

/**
 * 报告预览上下文（一次性把公共查询做完，避免每个位点重复查库）
 * <p>
 * 用 @Data 类而不是 record：字段有 13 个，record 的构造参数会被可读性规则判为「参数过多」。
 *
 * @author <你的名字>
 */
@Data
class PreviewContext {

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

    private String projectCode;

    private List<String> productGenes;

    private String specimenType;

}
