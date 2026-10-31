import { request } from '@/service/request';

/**
 * 样本备份接口层（页面只读）
 * <p>
 * 数据由宿主机脚本上报到 backup_sample_file（一物理文件一行），
 * 列表页的「样本一行」由后端按 sample_id 聚合，前端只展示，不做任何写操作。
 */

/** 样本备份列表（样本维度聚合） */
export function fetchGetSampleList(params: Api.Backup.SampleSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Backup.SampleRow>>({
    url: '/backup/sample/list',
    method: 'get',
    params
  });
}

/** 某个样本的文件明细 */
export function fetchGetSampleFiles(sampleId: number) {
  return request<Api.Backup.SampleFile[]>({
    url: `/backup/sample/${sampleId}/files`,
    method: 'get'
  });
}

/** 产品下拉选项 */
export function fetchGetSampleProductOptions() {
  return request<string[]>({
    url: '/backup/sample/product/options',
    method: 'get'
  });
}
