import { request } from '@/service/request';

/**
 * 产品配置接口层
 * <p>
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 后端返回 R<T> / TableDataInfo<T> 的分页结构，统一由 request 处理。
 */

/** 分页查询产品配置 */
export function fetchGetProductConfigList(params: Api.Biotech.ProductConfigSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Biotech.ProductConfig>>({
    url: '/biotech/productConfig/list',
    method: 'get',
    params
  });
}

/** 查询启用中的产品（供其它页面下拉使用） */
export function fetchGetActiveProductConfigList() {
  return request<Api.Biotech.ProductConfig[]>({
    url: '/biotech/productConfig/activeList',
    method: 'get'
  });
}

/** 查询详情 */
export function fetchGetProductConfig(id: number) {
  return request<Api.Biotech.ProductConfig>({
    url: `/biotech/productConfig/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateProductConfig(data: Api.Biotech.ProductConfigForm) {
  return request<null>({
    url: '/biotech/productConfig',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateProductConfig(data: Api.Biotech.ProductConfigForm) {
  return request<null>({
    url: '/biotech/productConfig',
    method: 'put',
    data
  });
}

/** 批量删除 */
export function fetchBatchDeleteProductConfig(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/biotech/productConfig/${ids.join(',')}`,
    method: 'delete'
  });
}
