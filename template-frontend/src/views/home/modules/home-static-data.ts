/**
 * 首页静态数据（按 biotech 首页的实现/数值复刻）
 * <p>
 * 后续接接口时**只改这个文件**：把导出的常量换成接口返回值即可，组件不用动。
 */
export interface StatCardItem {
  /** 卡片标题 */
  title: string;
  /** 主数字（格式化后的字符串） */
  total: string;
  /** 底部左侧文案 */
  footerLabel: string;
  /** 底部右侧文案 */
  footerValue: string;
  /** 卡内容形态：折线 / 面积 / 柱状 / 进度条 */
  variant: 'trend' | 'area' | 'bar' | 'progress';
  /** 迷你图数据（progress 形态不用） */
  data?: number[];
}

/** 顶部 4 张统计卡 */
export const STAT_CARDS: StatCardItem[] = [
  { title: '总报告数', total: '126,560', footerLabel: '今日生成', footerValue: '12,423', variant: 'trend' },
  {
    title: '样本总量',
    total: '88,460',
    footerLabel: '今日入库',
    footerValue: '1,234',
    variant: 'area',
    data: [7, 5, 4, 2, 4, 7, 5, 6, 5, 9, 6, 3, 1, 5, 3, 6, 5]
  },
  {
    title: '今日新增报告',
    total: '6,560',
    footerLabel: '审核通过率',
    footerValue: '60%',
    variant: 'bar',
    data: [7, 5, 4, 2, 4, 7, 5, 6, 5, 9, 6, 3, 1, 5, 3, 6, 5]
  },
  { title: '报告完成率', total: '78%', footerLabel: '周同比 12% ▲', footerValue: '日同比 11% ▼', variant: 'progress' }
];

/** 报告完成率进度条百分比 */
export const REPORT_DONE_PERCENT = 78;

/** 12 个月（横轴标签） */
export const MONTH_LABELS = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];

/** 报告维度的月度量 */
export const MONTH_REPORT_COUNT = [482, 438, 525, 580, 620, 560, 720, 638, 694, 816, 786, 860];

/** 样本维度的月度量 */
export const MONTH_SAMPLE_COUNT = [690, 590, 720, 660, 780, 740, 810, 820, 890, 980, 910, 1060];

export interface RankItem {
  title: string;
  total: number;
}

/** 报告类型排行 */
export const REPORT_TYPE_RANK: RankItem[] = [
  { title: '病理检测报告', total: 3232 },
  { title: '基因测序报告', total: 2934 },
  { title: '免疫组化报告', total: 2649 },
  { title: '分子诊断报告', total: 2387 },
  { title: '细胞学报告', total: 2074 },
  { title: '生化检测报告', total: 1943 },
  { title: '其他检测报告', total: 1768 }
];

/** 样本类型排行 */
export const SAMPLE_TYPE_RANK: RankItem[] = [
  { title: '血液样本', total: 4286 },
  { title: '组织样本', total: 3698 },
  { title: '唾液样本', total: 3182 },
  { title: '细胞样本', total: 2754 },
  { title: '尿液样本', total: 2316 },
  { title: '体液样本', total: 2058 },
  { title: '其他样本', total: 1824 }
];

export interface ProportionItem {
  name: string;
  value: number;
}

/** 类型占比饼图（报告维度 / 样本维度） */
export const TYPE_PROPORTION: Record<'report' | 'sample', ProportionItem[]> = {
  report: [
    { name: '病理检测', value: 4544 },
    { name: '基因测序', value: 3321 },
    { name: '免疫组化', value: 3113 },
    { name: '分子诊断', value: 2341 },
    { name: '细胞学', value: 1231 },
    { name: '其他', value: 1231 }
  ],
  sample: [
    { name: '血液样本', value: 4204 },
    { name: '组织样本', value: 3000 },
    { name: '唾液样本', value: 2302 },
    { name: '细胞样本', value: 1900 },
    { name: '尿液样本', value: 1110 },
    { name: '其他', value: 820 }
  ]
};

export interface SearchRankItem {
  index: number;
  keyword: string;
  count: number;
  /** 周涨幅（%） */
  range: number;
  /** 涨跌方向：up 红▲ / down 绿▼ */
  status: 'up' | 'down';
}

/** 检索维度排行 */
export const SEARCH_RANK: SearchRankItem[] = [
  ['报告编号查询', 648, 34],
  ['患者信息查询', 521, 26],
  ['基因检测结果', 489, 18],
  ['病理诊断结果', 445, 15],
  ['样本编号查询', 399, 12],
  ['检测项目查询', 368, 9],
  ['报告状态查询', 322, 8],
  ['签发医生查询', 287, 6],
  ['异常指标查询', 253, 5],
  ['历史报告查询', 219, 3]
].map(([keyword, count, range], i) => ({
  index: i + 1,
  keyword: keyword as string,
  count: count as number,
  range: range as number,
  // 与 biotech 一致：第 2/5/8 行显示下跌（绿▼），其余上涨（红▲）
  status: (i === 1 || i === 4 || i === 7 ? 'down' : 'up') as 'up' | 'down'
}));

/** 检索概览两个指标 */
export const SEARCH_SUMMARY = {
  count: { value: 12321, trend: '17.1', status: 'up' as const, data: [1, 6, 4, 8, 3, 7, 2] },
  avg: { value: 2.7, trend: '26.2', status: 'down' as const, data: [5, 3, 7, 2, 6, 4, 8] }
};

/** 千分位格式化（与 biotech 首页一致，用 Intl） */
export function formatNumber(value: number) {
  return new Intl.NumberFormat('zh-CN').format(value);
}
