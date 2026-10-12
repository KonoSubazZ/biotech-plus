import { request } from '@/service/request';

/**
 * 生信质控接口层
 * <p>
 * 页面只读：数据全部由上游通过固定 token 调 POST /qc/bioinfo/receive 推入，
 * 所以这里只有查询（列表 / 详情 / 样本编号→药企解析）。
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 */

/** 分页查询生信质控列表 */
export function fetchGetBioinfoQcList(params: Api.Qc.BioinfoQcSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Qc.BioinfoQc>>({
    url: '/qc/bioinfo/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetBioinfoQc(id: number) {
  return request<Api.Qc.BioinfoQc>({
    url: `/qc/bioinfo/${id}`,
    method: 'get'
  });
}

/** 样本编号 → 药企解析（排查「接口推数据被拒收」用，静默查 sample_file 与 sys_tenant） */
export function fetchResolveBioinfoQcTenant(subbarcode: string) {
  return request<Api.Qc.BioinfoQcTenant>({
    url: '/qc/bioinfo/tenant',
    method: 'get',
    params: { subbarcode }
  });
}

/** 修改（人工修正指标值；样本编号不可改，后端会按阈值重新判状态） */
export function fetchUpdateBioinfoQc(data: Api.Qc.BioinfoQcForm) {
  return request<null>({
    url: '/qc/bioinfo',
    method: 'put',
    data
  });
}

/** 批量删除（逻辑删除，传一个 id 的数组即单条删除） */
export function fetchBatchDeleteBioinfoQc(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/qc/bioinfo/${ids.join(',')}`,
    method: 'delete'
  });
}
