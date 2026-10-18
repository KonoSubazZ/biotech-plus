import { request } from '@/service/request';

export function fetchGetDataAuditList(params: Api.Audit.SearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Audit.Event>>({
    url: '/compliance/dataAudit/list',
    method: 'get',
    params
  });
}

export function fetchGetDataAuditDetail(id: CommonType.IdType) {
  return request<Api.Audit.Detail>({ url: `/compliance/dataAudit/${id}`, method: 'get' });
}
