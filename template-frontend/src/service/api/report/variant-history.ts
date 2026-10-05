import { request } from '@/service/request';

/**
 * 历史检出分页列表
 * <p>
 * 只读：数据在「提交审核」时由后端写进 report_variant_history（页面不提供增删改）。
 * 用药信息后端已解析成 8 个平铺字段（drugsA…resistantDrugsD），页面不再解析原始 JSON。
 */
export function fetchGetVariantHistoryList(params: Api.Report.VariantHistorySearchParams) {
  return request<Api.Report.VariantHistoryList>({
    url: '/report/variantHistory/list',
    method: 'get',
    params
  });
}
