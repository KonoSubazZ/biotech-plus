/**
 * 样本流转 9 个页签口径（唯一真相）：
 *   name  —— 页签名，同时是列表接口 tab 参数的取值（/report/flow/list?tab=driver）
 *   label —— 页签中文名
 *   perm  —— 该页签的权限点，与后端 sys_menu 的 report:flow:* 逐字一致
 *   hint  —— 该页签「处理」做什么（页签下方提示文案）
 *
 * 权限口径：有页签权限 = 页签内的按钮与接口都能用（与报告解读详情页同口径）；
 * 处理按钮额外要求 report:flow:process。
 */
export const SAMPLE_FLOW_TABS = [
  { name: 'driver', label: '驱动异常', perm: 'report:flow:driver', hint: '处理 = 打开「集群对接」，按文件重新驱动' },
  { name: 'bioinfo-view', label: '生信看点', perm: 'report:flow:bioinfo-view', hint: '处理 = 打开「筛选位点」开始看点' },
  { name: 'bioinfo-review', label: '生信审核', perm: 'report:flow:bioinfo-review', hint: '处理 = 打开「筛选位点」做生信复核' },
  { name: 'pending-interpret', label: '报告待解读', perm: 'report:flow:pending-interpret', hint: '处理 = 认领，认领后流转到「报告解读中」' },
  { name: 'interpreting', label: '报告解读中', perm: 'report:flow:interpreting', hint: '处理 = 打开「报告预览」生成/提交解读' },
  { name: 'pending-review', label: '报告待审核', perm: 'report:flow:pending-review', hint: '处理 = 认领，认领后流转到「报告审核中」' },
  { name: 'reviewing', label: '报告审核中', perm: 'report:flow:reviewing', hint: '处理 = 打开「审核报告」通过/驳回' },
  {
    name: 'signing',
    label: '报告签名',
    perm: 'report:flow:signing',
    hint: '审核通过后自动送签；签名失败（52）时处理 = 重新送签，签名中（50）等待签名回调'
  },
  { name: 'sending', label: '报告发送', perm: 'report:flow:sending', hint: '处理 = 打开「报告发送」发出报告' }
] as const;

/** 9 个页签的权限点集合（页面判断「处理」按钮与页签可见性用它） */
export const SAMPLE_FLOW_TAB_PERMS: string[] = SAMPLE_FLOW_TABS.map(tab => tab.perm);

/** 「处理」按钮权限点 */
export const SAMPLE_FLOW_PROCESS_PERM = 'report:flow:process';
