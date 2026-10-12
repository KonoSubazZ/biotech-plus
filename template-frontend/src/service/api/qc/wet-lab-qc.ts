import { request } from '@/service/request';

/**
 * 湿实验质控接口层
 * <p>
 * 数据由上游接口推送（POST /qc/wetLabQc/receive，不在前端调用），页面只查列表与删除。
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 */

/** 分页查询湿实验质控 */
export function fetchGetWetLabQcList(params: Api.Qc.WetLabQcSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Qc.WetLabQc>>({
    url: '/qc/wetLabQc/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetWetLabQc(id: number) {
  return request<Api.Qc.WetLabQc>({
    url: `/qc/wetLabQc/${id}`,
    method: 'get'
  });
}

/** 人工修改质控值（只传改了/要改的列；样本编号不可改，status 由后端按质控标准重判） */
export function fetchUpdateWetLabQc(data: Api.Qc.WetLabQcForm) {
  return request<null>({
    url: '/qc/wetLabQc',
    method: 'put',
    data
  });
}

/** 批量删除 */
export function fetchBatchDeleteWetLabQc(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/qc/wetLabQc/${ids.join(',')}`,
    method: 'delete'
  });
}
