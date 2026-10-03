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

/** 进入解读：创建/复用该分析批次的「解读中」报告，返回 reportId */
export function fetchEnterInterpretation(data: { analysisId: number }) {
  return request<Api.Report.InterpretationReport>({
    url: '/report/interpretation/enter',
    method: 'post',
    data
  });
}
