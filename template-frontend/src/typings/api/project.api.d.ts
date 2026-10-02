/**
 * 产品配置类型定义
 * <p>
 * 放 src/typings/ 下，命名空间 Api.<模块>.<实体>，与 api.ts、index.vue 共用。
 * 不要写成多份、也不要在组件里就地声明。
 */
declare namespace Api {
  namespace Project {
    /** 列表项（= 后端 Vo） */
    interface ProductConfig {
      id: number;
      name: string;
      code: string;
      /** active 启用 / inactive 停用 */
      status: string;
      testType?: string | null;
      relatedDiseases?: string | null;
      reportCycleDays?: number | null;
      remark?: string | null;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type ProductConfigList = Common.PaginatingQueryRecord<ProductConfig>;

    /** 搜索参数（与项目其它模块同一写法：分页走 Common.CommonSearchParams） */
    type ProductConfigSearchParams = CommonType.RecordNullable<
      Pick<ProductConfig, 'name' | 'code' | 'testType' | 'status'> & Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 Bo） */
    interface ProductConfigForm {
      id?: number | null;
      name: string;
      code: string;
      testType?: string | null;
      relatedDiseases?: string | null;
      reportCycleDays?: number | null;
      status?: string | null;
      remark?: string | null;
    }

    // ------------------------------------------------------------------ 产品关联基因

    /** 产品关联基因（= 后端 ProductGeneVo） */
    interface ProductGene {
      id: number;
      productId: number;
      /** 基因 id（来自 nkb.ncbi_gene） */
      geneId: number;
      geneSymbol: string;
      remark?: string | null;
      createTime?: string | null;
    }

    /** 关联基因列表的查询参数 */
    type ProductGeneSearchParams = CommonType.RecordNullable<
      Pick<ProductGene, 'productId' | 'geneSymbol'> & Common.CommonSearchParams
    >;

    /** 基因库搜索命中的条目（= 后端 NcbiGeneVo，只读不落库） */
    interface NcbiGene {
      geneId: number;
      geneSymbol: string;
      description?: string | null;
      synonyms?: string | null;
    }

    /** 批量导入参数：genes（搜索勾选，带 geneId）与 symbols（粘贴/Excel）二选一或同时给 */
    interface ProductGeneImport {
      productId: number;
      genes?: { geneId: number; geneSymbol: string }[];
      symbols?: string[];
    }

    /** 导入结果（= 后端 ProductGeneImportResultVo） */
    interface ProductGeneImportResult {
      addedCount: number;
      skippedCount: number;
      /** 基因库里查不到、未入库的 symbol */
      unmatched: string[];
      /** 一个 symbol 对应多个 gene_id 的（已按 gene_id 最小的关联） */
      ambiguous: string[];
    }
  }
}
