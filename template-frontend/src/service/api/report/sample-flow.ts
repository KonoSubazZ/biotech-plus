import { request } from '@/service/request';

/**
 * 样本流转接口层（报告管理 › 样本流转）
 * <p>
 * 约定同其它模块：函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 */

/** 分页查询样本流转列表（按页签 tab 给不同的状态与可见性过滤） */
export function fetchGetSampleFlowList(params: Api.Report.SampleFlowSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Report.SampleFlowRow>>({
    url: '/report/flow/list',
    method: 'get',
    params
  });
}

/** 页签「处理」：跳转报告解读对应页签（返回 targetTab + reportId），或就地认领（更新状态） */
export function fetchHandleSampleFlow(data: Api.Report.SampleFlowHandleParams) {
  return request<Api.Report.SampleFlowHandleResult>({
    url: '/report/flow/handle',
    method: 'post',
    data
  });
}
