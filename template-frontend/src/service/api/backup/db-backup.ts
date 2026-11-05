import { request } from '@/service/request';

/**
 * 数据库备份接口层（页面只读）
 * <p>
 * 页面上没有任何写操作；上报接口 /backup/db/full|binlog|prune 只给宿主备份脚本用，
 * 前端不调用、也不在权限里授给业务角色。
 */

/** 全量备份列表 */
export function fetchGetDbFullList(params: Api.Backup.DbBackupSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Backup.DbFull>>({
    url: '/backup/db/full/list',
    method: 'get',
    params
  });
}

/** binlog 增量列表 */
export function fetchGetDbBinlogList(params: Api.Backup.DbBackupSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Backup.DbBinlog>>({
    url: '/backup/db/binlog/list',
    method: 'get',
    params
  });
}

/** 汇总（四张卡片） */
export function fetchGetDbBackupSummary() {
  return request<Api.Backup.DbSummary>({
    url: '/backup/db/summary',
    method: 'get'
  });
}
