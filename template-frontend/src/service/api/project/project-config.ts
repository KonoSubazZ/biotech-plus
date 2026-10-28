import { request } from '@/service/request';

/**
 * 项目配置接口层
 * <p>
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 后端返回 R<T> / TableDataInfo<T> 的分页结构，统一由 request 处理。
 */

/** 分页查询项目配置 */
export function fetchGetProjectConfigList(params: Api.Project.ProjectConfigSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Project.ProjectConfig>>({
    url: '/project/projectConfig/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetProjectConfig(id: number) {
  return request<Api.Project.ProjectConfig>({
    url: `/project/projectConfig/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateProjectConfig(data: Api.Project.ProjectConfigForm) {
  return request<null>({
    url: '/project/projectConfig',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateProjectConfig(data: Api.Project.ProjectConfigForm) {
  return request<null>({
    url: '/project/projectConfig',
    method: 'put',
    data
  });
}

/** 批量删除 */
export function fetchBatchDeleteProjectConfig(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/project/projectConfig/${ids.join(',')}`,
    method: 'delete'
  });
}

/** 某项目下的研究中心列表（展开区用） */
export function fetchGetProjectCenters(id: number) {
  return request<Api.Project.ProjectConfig[]>({
    url: `/project/projectConfig/${id}/centers`,
    method: 'get'
  });
}

/** 新增研究中心 */
export function fetchCreateProjectCenter(data: Api.Project.ProjectCenterForm) {
  return request<null>({
    url: '/project/projectConfig/center',
    method: 'post',
    data
  });
}

/** 修改研究中心 */
export function fetchUpdateProjectCenter(data: Api.Project.ProjectCenterForm) {
  return request<null>({
    url: '/project/projectConfig/center',
    method: 'put',
    data
  });
}

/** 批量删除研究中心（单条删除也走这里，传一个 id 的数组） */
export function fetchBatchDeleteProjectCenter(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/project/projectConfig/center/${ids.join(',')}`,
    method: 'delete'
  });
}
