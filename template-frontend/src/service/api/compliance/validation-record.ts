import { request } from '@/service/request';

/**
 * 3Q 验证记录接口层
 * <p>
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 后端返回 R<T> / TableDataInfo<T> 的分页结构，统一由 request 处理。
 * append-only：不提供删除接口（合规上验证记录不可删改）。
 */

/** 分页查询验证记录 */
export function fetchGetValidationRecordList(params: Api.Compliance.ValidationRecordSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Compliance.ValidationRecord>>({
    url: '/compliance/validationRecord/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetValidationRecord(id: number) {
  return request<Api.Compliance.ValidationRecord>({
    url: `/compliance/validationRecord/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateValidationRecord(data: Api.Compliance.ValidationRecordForm) {
  return request<null>({
    url: '/compliance/validationRecord',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateValidationRecord(data: Api.Compliance.ValidationRecordForm) {
  return request<null>({
    url: '/compliance/validationRecord',
    method: 'put',
    data
  });
}
