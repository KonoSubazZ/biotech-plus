import { request } from '@/service/request';

/**
 * 开发记录接口层
 * <p>
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 后端返回 R<T> / TableDataInfo<T> 的分页结构，统一由 request 处理。
 * append-only：不提供删除接口（合规上开发记录不可删改）。
 */

/** 分页查询开发记录 */
export function fetchGetDevLogList(params: Api.Compliance.DevLogSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Compliance.DevLog>>({
    url: '/compliance/devLog/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetDevLog(id: number) {
  return request<Api.Compliance.DevLog>({
    url: `/compliance/devLog/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateDevLog(data: Api.Compliance.DevLogForm) {
  return request<null>({
    url: '/compliance/devLog',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateDevLog(data: Api.Compliance.DevLogForm) {
  return request<null>({
    url: '/compliance/devLog',
    method: 'put',
    data
  });
}
