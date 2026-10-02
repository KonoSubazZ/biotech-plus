import { request } from '@/service/request';

export function fetchGetTenantList(params?: Api.System.TenantSearchParams) {
  return request<Api.System.TenantList>({ url: '/system/tenant/list', method: 'get', params });
}

export function fetchTenantOptions() {
  return request<Api.System.Tenant[]>({ url: '/system/tenant/options', method: 'get' });
}

export function fetchCreateTenant(data: Api.System.TenantOperateParams) {
  return request<void>({ url: '/system/tenant', method: 'post', data });
}

export function fetchUpdateTenant(data: Api.System.TenantOperateParams) {
  return request<void>({ url: '/system/tenant', method: 'put', data });
}

export function fetchDeleteTenants(ids: CommonType.IdType[]) {
  return request<void>({ url: `/system/tenant/${ids.join(',')}`, method: 'delete' });
}
