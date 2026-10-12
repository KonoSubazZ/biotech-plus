/**
 * 位点等级计数（报告预览 / 审核页共用）。
 * <p>
 * 口径：体细胞取位点等级 `variationClass`（I类/II类/III类），胚系取临床意义 `clinicalSignificanceLabel`
 * （致病/可能致病/…）；只返回有数据的等级，label 形如「II类1」「致病2」。
 * 已知等级按固定顺序在前，未知等级（兜底）按出现顺序附后。
 */

/** 体细胞位点等级顺序（对齐 NkbDrugMatcher.variationClass：I类/II类/III类） */
export const SOMATIC_GRADE_ORDER = ['I类', 'II类', 'III类'];

/** 胚系临床意义顺序（对齐 1致病/2可能致病/3未知临床意义/4可能良性/5良性） */
export const GERMLINE_SIGNIFICANCE_ORDER = ['致病', '可能致病', '未知临床意义', '可能良性', '良性'];

/** 一个等级计数项 */
export interface GradeCount {
  /** 等级原文（I类 / 致病…） */
  key: string;
  /** 展示文案：等级 + 个数（II类1 / 致病2…） */
  label: string;
  /** 该等级的位点个数 */
  count: number;
}

/**
 * 按等级统计位点个数。
 *
 * @param section 位点分节（预览/审核页拿到的 somaticVariants / germlineVariants）
 * @param kind    somatic = 按位点等级；germline = 按临床意义
 * @return 等级计数项（只含有数据的等级）
 */
export function gradeCountTags(
  section: Api.Report.PreviewSection | undefined,
  kind: 'somatic' | 'germline'
): GradeCount[] {
  const order = kind === 'germline' ? GERMLINE_SIGNIFICANCE_ORDER : SOMATIC_GRADE_ORDER;
  const counts = new Map<string, number>();
  for (const item of section?.items ?? []) {
    const grade = kind === 'germline' ? item.clinicalSignificanceLabel : item.variationClass;
    if (!grade) {
      continue;
    }
    counts.set(grade, (counts.get(grade) ?? 0) + 1);
  }
  const known = order.filter(grade => counts.has(grade));
  const unknown = [...counts.keys()].filter(grade => !order.includes(grade));
  return [...known, ...unknown].map(grade => ({
    key: grade,
    label: `${grade}${counts.get(grade) ?? 0}`,
    count: counts.get(grade) ?? 0
  }));
}
