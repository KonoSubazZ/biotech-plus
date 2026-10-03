/**
 * 「报告解读」共用的状态 → 展示映射
 * <p>
 * 配色契约（本仓统一）：绿=正常/完成/通过，黄=进行中/待处理，红=非正常（驳回/失败/停用），
 * 灰只留给「未知值兜底」；未知值必须原样显示，不能静默按正常颜色渲染。
 * 列表页与详情页共用这一张表，避免同一状态两处两种颜色。
 */

export type StatusTagType = 'success' | 'warning' | 'error';

export interface StatusMeta {
  label: string;
  type: StatusTagType;
}

/** analysis_report.status 状态机 */
export const REPORT_STATUS_META: Record<string, StatusMeta> = {
  INTERPRETING: { label: '解读中', type: 'warning' },
  PENDING_REVIEW: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已审核', type: 'success' },
  REJECTED: { label: '已驳回', type: 'error' },
  SENT: { label: '已发送', type: 'success' }
};

/** data_file_status.status 集群文件状态（Tab② 集群对接） */
export const FILE_STATUS_META: Record<string, StatusMeta> = {
  Loaded: { label: '已加载', type: 'success' },
  Pending: { label: '处理中', type: 'warning' },
  Error: { label: '失败', type: 'error' }
};

/** analysis_data.drive_status 集群驱动状态 */
export const DRIVE_STATUS_META: Record<string, StatusMeta> = {
  DRIVING: { label: '驱动中', type: 'warning' },
  LOADED: { label: '已完成', type: 'success' },
  PARTIAL: { label: '部分成功', type: 'error' },
  FAILED: { label: '失败', type: 'error' }
};

/** 未知状态兜底：灰 + 原样显示（null 时用 fallbackLabel） */
export function statusMeta(map: Record<string, StatusMeta>, value: string | null, fallbackLabel = '-') {
  const hit = map[value ?? ''];
  if (hit) {
    return hit;
  }
  return { label: value || fallbackLabel, type: 'default' as const };
}
