/**
 * 报告解读详情页的 6 个页签口径（唯一真相）：
 *   name  —— 页签名，同时是路由 tab 参数的取值（/report/interpretation?reportId=..&analysisId=..&tab=review）
 *   label —— 页签中文名（与设计说明书表 4-1 一致）
 *   perm  —— 该页签的权限点，与后端 InterpretationController 的 @SaCheckPermission 逐字一致
 *
 * 权限口径：有页签权限 = 页签内的按钮与接口都能用（接口侧挂同一权限点）。
 */
export const INTERPRETATION_TABS = [
  { name: 'lims', label: 'LIMS 信息', perm: 'report:interpretation:lims' },
  { name: 'files', label: '集群对接', perm: 'report:interpretation:files' },
  { name: 'variants', label: '筛选位点', perm: 'report:interpretation:variants' },
  { name: 'preview', label: '报告预览', perm: 'report:interpretation:preview' },
  { name: 'review', label: '审核报告', perm: 'report:interpretation:review' },
  { name: 'send', label: '报告发送', perm: 'report:interpretation:send' }
] as const;

/** 6 个页签的权限点集合：列表页判断「解读」按钮是否可见用它 */
export const INTERPRETATION_TAB_PERMS: string[] = INTERPRETATION_TABS.map(tab => tab.perm);
