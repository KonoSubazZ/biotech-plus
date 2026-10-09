import { request } from '@/service/request';

/**
 * 报告解读接口层（报告管理 › 报告解读）
 * <p>
 * 约定同其它模块：函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 位点筛选 / 预览 / 审核 / 发送的接口在后续页面往这里加。
 */

/** 分页查询报告解读列表（analysis_data ⟕ 最新 analysis_report） */
export function fetchGetInterpretationList(params: Api.Report.InterpretationSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Report.InterpretationRow>>({
    url: '/report/interpretation/list',
    method: 'get',
    params
  });
}

/** 解读页上下文：报告头 + LIMS 信息（Tab①）+ 生成前校验结论 */
export function fetchGetInterpretationContext(params: { reportId: number; analysisId: number }) {
  return request<Api.Report.InterpretationContext>({
    url: '/report/interpretation/context',
    method: 'get',
    params
  });
}

/** Tab② 集群对接：某分析批次的文件分页列表 */
export function fetchGetInterpretationFileList(params: Api.Report.InterpretationFileSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Report.InterpretationFile>>({
    url: '/report/interpretation/files',
    method: 'get',
    params
  });
}

/** Tab② 查看单个文件内容（超长已在后端截断） */
export function fetchGetInterpretationFileContent(params: { fileId: number; analysisId: number }) {
  return request<Api.Report.InterpretationFileContent>({
    url: '/report/interpretation/file/content',
    method: 'get',
    params
  });
}

/** Tab③ 筛选位点：按类型分页查询位点 */
export function fetchGetInterpretationVariantList(params: Api.Report.InterpretationVariantSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Report.InterpretationVariant>>({
    url: '/report/interpretation/variants',
    method: 'get',
    params
  });
}

/** Tab③ 切换「入报告」开关（写共享的 file_*.is_reported） */
export function fetchUpdateVariantReportStatus(data: {
  analysisId: number;
  sourceType: string;
  sourceId: number;
  isReported: number;
  filteredRationale?: string | null;
}) {
  return request<null>({
    url: '/report/interpretation/variant-report-status',
    method: 'post',
    data
  });
}

/** 人工选模板：报告产品对应的候选模板列表（默认模板排最前） */
export function fetchGetInterpretationTemplateOptions(params: { analysisId: number; reportId: number }) {
  return request<Api.Report.InterpretationTemplateOption[]>({
    url: '/report/interpretation/template-options',
    method: 'get',
    params
  });
}

/**
 * Tab④ 报告预览：报告已生成过且模板一致时，后端直接读那份已落库的 JSON 渲染（免重复匹配），
 * 否则实时组装。只返回、不落库。
 */
export function fetchBuildInterpretationPreview(data: {
  analysisId: number;
  reportId: number;
  templateId?: number;
}) {
  return request<Api.Report.InterpretationPreviewResult>({
    url: '/report/interpretation/preview',
    method: 'post',
    data
  });
}

/** 人工确认胚系五级临床意义 */
export function fetchUpdateGermlineSignificance(data: {
  analysisId: number;
  reportId: number;
  sourceId: number;
  clinicalSignificance: number;
}) {
  return request<null>({
    url: '/report/interpretation/germline-clinical-significance',
    method: 'post',
    data
  });
}

/** 改靶（体细胞 / 胚系共用）：给位点人工指定一个或多个 NKB 父级节点；空数组 = 取消改靶 */
export function fetchUpdateVariantTarget(data: {
  analysisId: number;
  reportId: number;
  sourceType: string;
  sourceId: number;
  parentMutationIds: number[];
}) {
  return request<null>({
    url: '/report/interpretation/variant-target',
    method: 'post',
    data
  });
}

/** 改靶候选：按关键词（基因符号 / 节点名片段）查 NKB 位点节点 */
export function fetchParentCandidates(keyword: string) {
  return request<Api.Report.NkbVariantNode[]>({
    url: '/report/interpretation/parent-candidates',
    method: 'get',
    params: { keyword }
  });
}

/** 进入解读：创建/复用该分析批次的「解读中」报告，返回 reportId */
export function fetchEnterInterpretation(data: { analysisId: number }) {
  return request<Api.Report.InterpretationReport>({
    url: '/report/interpretation/enter',
    method: 'post',
    data
  });
}
