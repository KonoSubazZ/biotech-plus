/**
 * 历史检出（报告管理 › 历史检出）类型定义
 * <p>
 * 与 report-interpretation.api.d.ts 分开声明；都落在 Api.Report 命名空间，TypeScript 自动合并。
 * 数据来源：提交审核时由后端写进 report_variant_history。
 */
declare namespace Api {
  namespace Report {
    /** 历史检出行（= 后端 ReportVariantHistoryVo） */
    interface VariantHistoryRow {
      id: number;
      /** 报告ID */
      reportId: number;
      /** 样本编号 */
      subbarcode: string | null;
      /** 项目编号（替代客户做归属展示） */
      projectNo: string | null;
      /** 客户（LIMS 医院名） */
      client: string | null;
      /** 产品 */
      product: string | null;
      /** 解读癌种 */
      disease: string | null;
      /** 来源：SNP_INDEL / CNV / FUSION / CR_ALL */
      sourceType: string | null;
      /** 基因 */
      gene: string | null;
      /** 规范化位点 */
      variant: string | null;
      /** 丰度（= 预览页「丰度/reads」列；无口径为空） */
      abundance: string | null;
      /** 突变类型（VEP ExonicFunc 中文：错义突变/移码突变…；无则空） */
      variantType: string | null;
      /** 原始位点描述 */
      oriVariant: string | null;
      /** 位点等级（胚系为临床意义标签） */
      level: string | null;
      /** 用药信息 · 获益A级（药名串，`;` 分隔；无则 null） */
      drugsA: string | null;
      /** 用药信息 · 获益B级 */
      drugsB: string | null;
      /** 用药信息 · 获益C级 */
      drugsC: string | null;
      /** 用药信息 · 获益D级 */
      drugsD: string | null;
      /** 用药信息 · 耐药A级 */
      resistantDrugsA: string | null;
      /** 用药信息 · 耐药B级 */
      resistantDrugsB: string | null;
      /** 用药信息 · 耐药C级 */
      resistantDrugsC: string | null;
      /** 用药信息 · 耐药D级 */
      resistantDrugsD: string | null;
      /** 解读人 */
      analyzer: string | null;
      /** 报告时间（报告生成时间） */
      reportGeneratedAt: string | null;
    }

    /** 历史检出分页列表 */
    type VariantHistoryList = Common.PaginatingQueryRecord<VariantHistoryRow>;

    /**
     * 历史检出搜索参数
     * <p>
     * 搜索栏 5 项：报告时间（范围 → params.beginTime/endTime，作用在 report_generated_at）、
     * 样本编号、解读癌种、产品、客户。
     */
    interface VariantHistorySearchParams {
      /** 分页（与 useNaivePaginatedTable 的分页回调同口径：可能为 undefined） */
      pageNum?: number | null;
      pageSize?: number | null;
      /** 样本编号 */
      subbarcode: string | null;
      /** 解读癌种 */
      disease: string | null;
      /** 产品 */
      product: string | null;
      /** 项目编号 */
      projectNo?: string | null;
      /** 客户 */
      client: string | null;
      /** 报告时间范围 */
      params: {
        beginTime?: string | null;
        endTime?: string | null;
      };
    }
  }
}
