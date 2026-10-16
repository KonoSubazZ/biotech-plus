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

/** Tab③ 看点完成：20 生信看点 → 21 生信审核（返回流转后的状态码） */
export function fetchFinishBioinfoView(data: { analysisId: number; reportId: number }) {
  return request<number>({
    url: '/report/interpretation/bioinfo/view-complete',
    method: 'post',
    data
  });
}

/** Tab③ 生信复核通过：21 生信审核 → 30 报告待解读（返回流转后的状态码） */
export function fetchFinishBioinfoReview(data: { analysisId: number; reportId: number }) {
  return request<number>({
    url: '/report/interpretation/bioinfo/review-complete',
    method: 'post',
    data
  });
}

/** Tab② 重新驱动单个文件（后端调外部 scanner.py 驱动单文件，脚本写回状态后返回最新值） */
export function fetchRedriveInterpretationFile(data: { analysisId: number; fileId: number }) {
  return request<Api.Report.FileRedriveResult>({
    url: '/report/interpretation/file/redrive',
    method: 'post',
    data
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

/** 报告生成参数 · 解读癌种候选（NKB 疾病分类） */
export function fetchGetDiseaseOptions() {
  return request<Api.Report.DiseaseOption[]>({
    url: '/report/interpretation/disease-options',
    method: 'get'
  });
}

/** 报告生成参数 · 解读人候选（启用中的系统用户） */
export function fetchGetAnalyzerOptions() {
  return request<Api.Report.AnalyzerOption[]>({
    url: '/report/interpretation/analyzer-options',
    method: 'get'
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
  /** 报告生成参数：模板 / 解读癌种 / 报告日期 / 解读人（都不传就用默认值） */
  templateId?: number;
  disease?: string;
  reportDate?: string;
  analyzer?: string;
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

/**
 * 正式生成报告：后端按「报告生成参数」重新组装 → 写 JSON/DOCX 到固定输出目录 → 登记 analysis_report。
 * 返回体含 reportName / docxFileName / docxPath（下载按钮用它做文件名兜底）。
 */
export function fetchGenerateReport(data: {
  analysisId: number;
  reportId: number;
  /** 报告生成参数：模板 / 解读癌种 / 报告日期 / 解读人（都不传就用默认值） */
  templateId?: number;
  disease?: string;
  reportDate?: string;
  analyzer?: string;
}) {
  return request<Record<string, unknown>>({
    url: '/report/interpretation/generate',
    method: 'post',
    data,
    // 生成是重活（组装 JSON + Python 渲染 DOCX + OnlyOffice 转 PDF）：单独放宽到 180 秒，与后端转换上限
    // （onlyoffice.timeout-seconds=180）对齐。全局是 30 秒（src/service/request/index.ts）；实测成功一次约 13 秒，
    // 但模板大 / 文档服务器排队时可能超过 30 秒，会给页面造成「报超时、后端其实已落库」的假失败。
    timeout: 180 * 1000
  });
}

/**
 * 已生成报告文件的下载地址（走鉴权，不能直接 <a href>）：
 * 交给 useDownload 的 zip() 或带 Authorization 的 fetch 取 blob 再用。
 */
export function fetchGetReportFileUrl(reportId: number, analysisId: number) {
  return `/report/interpretation/report-file?reportId=${reportId}&analysisId=${analysisId}`;
}

/**
 * 提交审核：冻结本次解读的位点匹配历史（history_*）并把报告置为 PENDING_REVIEW。
 * 参数与生成接口同一套（冻结结果依赖癌种范围，必须与生成时一致）。
 */
export function fetchSubmitReview(data: {
  analysisId: number;
  reportId: number;
  /** 报告生成参数：模板 / 解读癌种 / 报告日期 / 解读人（都不传就用默认值） */
  templateId?: number;
  disease?: string;
  reportDate?: string;
  analyzer?: string;
}) {
  return request<null>({
    url: '/report/interpretation/submit',
    method: 'post',
    data
  });
}

/**
 * 审核通过：PENDING_REVIEW → APPROVED。
 * 服务端校验：只有待审核能通过、产出人不能审核自己的报告。
 */
export function fetchApproveReview(data: { analysisId: number; reportId: number }) {
  return request<null>({
    url: '/report/interpretation/approve',
    method: 'post',
    data
  });
}

/**
 * 驳回：PENDING_REVIEW → REJECTED。
 * reason 必填（服务端会校验并写进 analysis_report.comment）。
 */
export function fetchRejectReview(data: { analysisId: number; reportId: number; reason: string }) {
  return request<null>({
    url: '/report/interpretation/reject',
    method: 'post',
    data
  });
}

/**
 * Tab⑥ 报告发送：发送页参数（收件/抄送候选 + 渲染后的标题正文 + 附件名 + 可否发送）。
 * 收件候选来自该报告 LIMS 的邮箱字段；标题正文是「邮件配置」模板渲染出的默认值，页面可改。
 */
export function fetchGetMailPreview(params: { reportId: number; analysisId: number }) {
  return request<Api.Report.InterpretationMailPreview>({
    url: '/report/interpretation/mail-preview',
    method: 'get',
    params
  });
}

/**
 * Tab⑥ 报告发送：把已审核的报告作为附件发出。
 * 服务端校验：只有 APPROVED 能发；成功后把报告置 SENT 并写发送人/时间。
 */
export function fetchSendReport(data: {
  analysisId: number;
  reportId: number;
  /** 用哪个发件邮箱（邮件配置ID）；不传 = 用「默认邮箱」 */
  configId?: number;
  /** 收件人（逗号 / 分号分隔） */
  to: string;
  /** 抄送（可空） */
  cc?: string;
  subject: string;
  content: string;
  /** 是否附带报告文件（默认 true） */
  attachReport?: boolean;
}) {
  return request<{ traceId: string; messageId: string; recipientCount: number }>({
    url: '/report/interpretation/send',
    method: 'post',
    data
  });
}
