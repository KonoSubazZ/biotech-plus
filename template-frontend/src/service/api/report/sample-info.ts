import { request } from '@/service/request';

/**
 * 样本信息接口层
 * <p>
 * 约定：一个实体一个文件；函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * Excel 导入走 NUpload 直传（见 views/report/sample-info/modules/sample-info-import-modal.vue），
 * 所以这里没有导入函数；导入模板下载同样由该弹窗用 useDownload 直接请求。
 */

/** 分页查询样本信息 */
export function fetchGetSampleInfoList(params: Api.Report.SampleInfoSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Report.SampleInfo>>({
    url: '/report/sampleInfo/list',
    method: 'get',
    params
  });
}

/** 查询详情 */
export function fetchGetSampleInfo(id: number) {
  return request<Api.Report.SampleInfo>({
    url: `/report/sampleInfo/${id}`,
    method: 'get'
  });
}

/** 新增 */
export function fetchCreateSampleInfo(data: Api.Report.SampleInfoForm) {
  return request<null>({
    url: '/report/sampleInfo',
    method: 'post',
    data
  });
}

/** 修改 */
export function fetchUpdateSampleInfo(data: Api.Report.SampleInfoForm) {
  return request<null>({
    url: '/report/sampleInfo',
    method: 'put',
    data
  });
}

/** 批量删除 */
export function fetchBatchDeleteSampleInfo(ids: CommonType.IdType[]) {
  return request<null>({
    url: `/report/sampleInfo/${ids.join(',')}`,
    method: 'delete'
  });
}
