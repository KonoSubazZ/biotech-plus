import { request } from '@/service/request';

/**
 * 3Q 文档管理接口层
 * <p>
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 后端返回 R<T> / TableDataInfo<T> 的分页结构，统一由 request 处理。
 * 记录类：提供批量删除（软删）。
 */

/** 分页查询文档列表 */
export function fetchGetComplianceDocList(params: Api.Compliance.ComplianceDocSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Compliance.ComplianceDoc>>({
    url: '/compliance/complianceDoc/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetComplianceDoc(id: number) {
  return request<Api.Compliance.ComplianceDoc>({
    url: `/compliance/complianceDoc/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateComplianceDoc(data: Api.Compliance.ComplianceDocForm) {
  return request<null>({
    url: '/compliance/complianceDoc',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateComplianceDoc(data: Api.Compliance.ComplianceDocForm) {
  return request<null>({
    url: '/compliance/complianceDoc',
    method: 'put',
    data
  });
}

/** 批量删除（软删） */
export function fetchBatchDeleteComplianceDoc(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/compliance/complianceDoc/${ids.join(',')}`,
    method: 'delete'
  });
}
