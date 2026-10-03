import { request } from '@/service/request';

/**
 * 质控记录接口层
 * <p>
 * 湿实验质控（/qc/wet-lab）与生信质控（/qc/bioinfo）共用本文件的接口，
 * 只是查询参数里的 qcCategory 不同（wet_lab / bioinfo）。
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 */

/** 分页查询质控记录 */
export function fetchGetQcRecordList(params: Api.Qc.QcRecordSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Qc.QcRecord>>({
    url: '/qc/qcRecord/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetQcRecord(id: number) {
  return request<Api.Qc.QcRecord>({
    url: `/qc/qcRecord/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateQcRecord(data: Api.Qc.QcRecordForm) {
  return request<null>({
    url: '/qc/qcRecord',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateQcRecord(data: Api.Qc.QcRecordForm) {
  return request<null>({
    url: '/qc/qcRecord',
    method: 'put',
    data
  });
}

/** 批量删除 */
export function fetchBatchDeleteQcRecord(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/qc/qcRecord/${ids.join(',')}`,
    method: 'delete'
  });
}
