import { request } from '@/service/request';

/**
 * 质控标准接口层
 * <p>
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 后端返回 R<T> / TableDataInfo<T> 的分页结构，统一由 request 处理。
 */

/** 分页查询质控标准 */
export function fetchGetQcStandardList(params: Api.Qc.QcStandardSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Qc.QcStandard>>({
    url: '/qc/qcStandard/list',
    method: 'get',
    params
  });
}

/** 查询启用中的质控标准（供其它页面下拉使用） */
export function fetchGetActiveQcStandardList() {
  return request<Api.Qc.QcStandard[]>({
    url: '/qc/qcStandard/activeList',
    method: 'get'
  });
}

/** 查询详情 */
export function fetchGetQcStandard(id: number) {
  return request<Api.Qc.QcStandard>({
    url: `/qc/qcStandard/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateQcStandard(data: Api.Qc.QcStandardForm) {
  return request<null>({
    url: '/qc/qcStandard',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateQcStandard(data: Api.Qc.QcStandardForm) {
  return request<null>({
    url: '/qc/qcStandard',
    method: 'put',
    data
  });
}

/** 批量删除 */
export function fetchBatchDeleteQcStandard(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/qc/qcStandard/${ids.join(',')}`,
    method: 'delete'
  });
}
