import { request } from '@/service/request';

/**
 * 报告模板配置接口层
 * <p>
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 分页响应是顶层 rows/total（用 Api.Common.PaginatingQueryRecord）。
 */

/** 分页查询报告模板 */
export function fetchGetReportTemplateList(params: Api.Report.ReportTemplateSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Report.ReportTemplate>>({
    url: '/report/template/list',
    method: 'get',
    params
  });
}

/** 查询详情（含关联产品） */
export function fetchGetReportTemplate(templateId: number) {
  return request<Api.Report.ReportTemplate>({
    url: `/report/template/${templateId}`,
    method: 'get'
  });
}

/** 产品下拉选项（模板表单的「关联产品」用） */
export function fetchGetProductOptions() {
  return request<Api.Report.ProductOption[]>({
    url: '/report/template/productOptions',
    method: 'get'
  });
}

/** 报告命名可用变量（模板表单的提示与试算用；同一份字典也是后端保存期校验口径） */
export function fetchGetReportNameVars() {
  return request<Api.Report.ReportNameVars>({
    url: '/report/template/name-vars',
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateReportTemplate(data: Api.Report.ReportTemplateForm) {
  return request<null>({
    url: '/report/template',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateReportTemplate(data: Api.Report.ReportTemplateForm) {
  return request<null>({
    url: '/report/template',
    method: 'put',
    data
  });
}

/** 批量删除 */
export function fetchBatchDeleteReportTemplate(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/report/template/${ids.join(',')}`,
    method: 'delete'
  });
}
