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

/** analysis_report.report_status 状态机（int 码，与后端 utils/ReportStatus 逐一对一） */
export const REPORT_STATUS_META: Record<string, StatusMeta> = {
  '20': { label: '生信看点', type: 'warning' },
  '21': { label: '生信审核', type: 'warning' },
  '30': { label: '报告待解读', type: 'warning' },
  '31': { label: '报告解读中', type: 'warning' },
  '32': { label: '报告生成成功', type: 'warning' },
  '33': { label: '报告生成失败', type: 'error' },
  '40': { label: '报告待审核', type: 'warning' },
  '41': { label: '报告审核中', type: 'warning' },
  '42': { label: '报告审核失败', type: 'error' },
  '43': { label: '报告审核成功', type: 'success' },
  '50': { label: '报告签名中', type: 'warning' },
  '52': { label: '报告签名失败', type: 'error' },
  '53': { label: '报告签名成功', type: 'success' },
  '60': { label: '报告待发送', type: 'warning' },
  '61': { label: '报告发送失败', type: 'error' },
  '62': { label: '报告发送成功', type: 'success' }
};

/** analysis_file_status.status 集群文件状态（Tab② 集群对接） */
export const FILE_STATUS_META: Record<string, StatusMeta> = {
  Loaded: { label: '已加载', type: 'success' },
  Pending: { label: '处理中', type: 'warning' },
  Error: { label: '失败', type: 'error' }
};

/** analysis_data.drive_status 集群驱动状态（int 码，与后端 utils/DriveStatus 逐一对一） */
export const DRIVE_STATUS_META: Record<string, StatusMeta> = {
  '10': { label: '驱动失败', type: 'error' },
  '11': { label: '驱动成功', type: 'success' },
  '12': { label: '驱动中', type: 'warning' }
};

/** 未知状态兜底：灰 + 原样显示（null 时用 fallbackLabel） */
export function statusMeta(map: Record<string, StatusMeta>, value: string | number | null, fallbackLabel = '-') {
  // 映射表的 key 一律是字符串；后端既可能给数字码（int 列）也可能给字符串，这里统一转一下
  const hit = map[value === null || value === undefined ? '' : String(value)];
  if (hit) {
    return hit;
  }
  return { label: value === null || value === undefined || value === '' ? fallbackLabel : String(value), type: 'default' as const };
}
