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
      /** 产品编码（对外，唯一） */
      productCode: string;
      /** 生信编码：= 上传目录第 2 层 product，与 analysis_data.product_bio 逐字相等 */
      productBio: string;
      /** 产品名称（对外展示、报告用） */
      productName: string;
      /** active 启用 / inactive 停用 */
      status: string;
      /** 检测类型（组织 / 血液） */
      testType?: string | null;
      /** panel类型（DNA / RNA / DNA_RNA / WES / HRD 等） */
      panelType?: string | null;
      relatedDiseases?: string | null;
      reportCycleDays?: number | null;
      remark?: string | null;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type ProductConfigList = Common.PaginatingQueryRecord<ProductConfig>;

    /** 搜索参数（与项目其它模块同一写法：分页走 Common.CommonSearchParams） */
    type ProductConfigSearchParams = CommonType.RecordNullable<
      Pick<ProductConfig, 'productName' | 'productBio' | 'productCode' | 'testType' | 'panelType' | 'status'> &
        Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 Bo） */
    interface ProductConfigForm {
      id?: number | null;
      productCode: string;
      productBio: string;
      productName: string;
      testType?: string | null;
      panelType?: string | null;
      relatedDiseases?: string | null;
      reportCycleDays?: number | null;
      status?: string | null;
      remark?: string | null;
    }

    // ------------------------------------------------------------------ 项目配置

    /** 项目配置（= 后端 ProjectConfigVo）：parentId=0 是项目行，>0 是研究中心行 */
    interface ProjectConfig {
      id: number;
      /** 父节点 id（0=项目行；>0=研究中心行） */
      parentId?: number | null;
      /** 项目编号（同一租户内唯一） */
      projectNo: string;
      /** 项目名称 */
      projectName: string;
      /** 客户名称 */
      customer: string;
      /** 合同名称（项目级展示字段，不参与业务逻辑） */
      contractName?: string | null;
      /** 合同编号（项目级展示字段） */
      contractNo?: string | null;
      /** 研究方案编号（项目级展示字段） */
      schemeNo?: string | null;
      /** 研究中心编号（仅研究中心行有值） */
      researchCenterNo?: string | null;
      /** 研究中心名称（仅研究中心行有值） */
      researchCenterName?: string | null;
      /** 收件邮箱（多个用分号分隔；报告发送用，页面已隐藏，仅接口保留） */
      receiveEmail: string;
      /** 项目通知收件邮箱（多个用分号分隔；页面上的「项目通知（收件）邮箱」） */
      projectNotifyEmail?: string | null;
      /** 抄送邮箱（多个用分号分隔） */
      ccEmail?: string | null;
      remark?: string | null;
      /** 创建人 id */
      createBy?: number | null;
      /** 创建人姓名（后端 @Translation 换出的昵称） */
      createByName?: string | null;
      createTime?: string | null;
      /** 更新人 id */
      updateBy?: number | null;
      /** 更新人姓名 */
      updateByName?: string | null;
      updateTime?: string | null;
    }

    /** 项目配置的搜索参数（搜索栏只有项目编号与客户名称） */
    type ProjectConfigSearchParams = CommonType.RecordNullable<
      Pick<ProjectConfig, 'projectNo' | 'customer'> & Common.CommonSearchParams
    >;

    /** 项目配置新增/编辑表单（= 后端 ProjectConfigBo） */
    interface ProjectConfigForm {
      id?: number | null;
      projectNo: string;
      projectName: string;
      customer: string;
      contractName?: string | null;
      contractNo?: string | null;
      schemeNo?: string | null;
      receiveEmail: string;
      /** 项目通知收件邮箱（可空） */
      projectNotifyEmail?: string | null;
      ccEmail?: string | null;
      remark?: string | null;
    }

    /**
     * 研究中心新增/编辑表单（= 后端 ProjectCenterBo）
     * <p>
     * 项目名 / 客户名 / 合同三项不在这里：它们只属于项目行，中心行写空。
     */
    interface ProjectCenterForm {
      id?: number | null;
      /** 所属项目行 id（新增必填） */
      parentId: number;
      researchCenterNo: string;
      researchCenterName: string;
      receiveEmail: string;
      ccEmail?: string | null;
      remark?: string | null;
    }

    /** 研究中心导入结果（= 后端 CenterImportResultVo）：先全量校验，任一行不过就整体不导入 */
    interface CenterImportResult {
      /** 解析到的数据行数（不含表头与整行空白） */
      total: number;
      /** 实际新增（committed=false 时恒为 0） */
      inserted: number;
      /** 实际修改（committed=false 时恒为 0） */
      updated: number;
      /** 校验失败条数 */
      failed: number;
      /** 是否真正写库 */
      committed: boolean;
      /** 每条失败原因（含 Excel 行号与编号） */
      errors: string[];
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
