import { request } from '@/service/request';

/**
 * 样本信息接口层（sample_biotech）
 * <p>
 * 页面路由仍是菜单「样本管理 › 样本信息」，后端实体换成 CmsSample，接口前缀 /report/cmsSample。
 * Excel 导入走 NUpload 直传（见 modules/sample-info-import-modal.vue）。
 */

/** 分页查询样本信息 */
export function fetchGetCmsSampleList(params: Api.Report.CmsSampleSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Report.CmsSample>>({
    url: '/report/cmsSample/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetCmsSample(id: number) {
  return request<Api.Report.CmsSample>({
    url: `/report/cmsSample/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateCmsSample(data: Api.Report.CmsSampleForm) {
  return request<null>({
    url: '/report/cmsSample',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateCmsSample(data: Api.Report.CmsSampleForm) {
  return request<null>({
    url: '/report/cmsSample',
    method: 'put',
    data
  });
}

/** 批量删除 */
export function fetchBatchDeleteCmsSample(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/report/cmsSample/${ids.join(',')}`,
    method: 'delete'
  });
}

/** 按样本编号（不是主键）从 CMS 刷新一条；列表快照过期时也能凭编号把行补回来 */
export function fetchRefreshCmsSample(subbarcode: string) {
  return request<Api.Report.CmsSampleRefresh>({
    url: '/report/cmsSample/refresh',
    method: 'post',
    params: { subbarcode }
  });
}

/** 历史修改信息：「从CMS更新」+「编辑保存」的变更流水，时间倒序 */
export function fetchGetCmsSampleChangeLog(id: number) {
  return request<Api.Report.CmsSampleChangeLog[]>({
    url: `/report/cmsSample/${id}/change-log`,
    method: 'get'
  });
}
