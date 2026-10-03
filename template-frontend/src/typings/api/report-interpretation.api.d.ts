/**
 * 报告解读（报告管理 › 报告解读）类型定义
 * <p>
 * 与 typings/api/report.api.d.ts 分开：那份已经有样本信息的 122 列（415 行），
 * 两份都声明 Api.Report 命名空间，TypeScript 会自动合并。
 */
declare namespace Api {
  namespace Report {
    // ---------------------------------------------------------------------
    // 报告解读（报告管理 › 报告解读）
    // 列表 = 一个分析批次（analysis_data）⟕ 该批次最新一份报告（analysis_report）
    // ---------------------------------------------------------------------

    /** 报告解读列表行（= 后端 InterpretationRowVo） */
    interface InterpretationRow {
      /** 分析数据ID */
      analysisId: number;
      /** 分析日期（YYYYMMDD） */
      analysisDate: string | null;
      /** 样本编号 */
      subbarcode: string | null;
      /** 患者编号 */
      barcode: string | null;
      /** 产品名称 */
      product: string | null;
      /** 产品ID（product_config.id） */
      productId: number | null;
      /** 解读人员 */
      analyzer: string | null;
      /** 集群驱动状态：DRIVING / LOADED / PARTIAL */
      driveStatus: string | null;
      /** 驱动执行时间 */
      driveExecutedAt: string | null;
      /** 报告ID（为空 = 该批次还没点过「解读」） */
      reportId: number | null;
      /** 报告状态：INTERPRETING / PENDING_REVIEW / APPROVED / REJECTED / SENT */
      reportStatus: string | null;
      /** 解读癌种 */
      analysisDisease: string | null;
      /** 模板ID */
      templateId: number | null;
      /** 报告模板编码 */
      template: string | null;
      /** 报告产出人 */
      reportGeneratedBy: string | null;
      /** 报告生成时间 */
      reportGeneratedAt: string | null;
    }

    /** 报告解读分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type InterpretationList = Common.PaginatingQueryRecord<InterpretationRow>;

    /**
     * 报告解读搜索参数
     * <p>
     * 搜索栏 4 项 → 字段：样本编号 subbarcode、产品 product、报告状态 reportStatus、
     * 分析日期走 params.beginTime / params.endTime（analysis_date 是 varchar(8) 的 YYYYMMDD，
     * 组件里已把 yyyy-MM-dd 转成 yyyyMMdd）。
     */
    type InterpretationSearchParams = CommonType.RecordNullable<
      Pick<InterpretationRow, 'subbarcode' | 'product' | 'reportStatus'> & Common.CommonSearchParams
    >;

    /** 报告头信息（= 后端 AnalysisReportVo，进详情页要用 reportId） */
    interface InterpretationReport {
      reportId: number;
      analysisId: number;
      analysisDate: string | null;
      subbarcode: string | null;
      product: string | null;
      productId: number | null;
      analysisDisease: string | null;
      templateId: number | null;
      template: string | null;
      status: string;
      comment: string | null;
      createTime: string | null;
    }

    /** 解读页 Tab① LIMS 信息（= 后端 InterpretationLimsVo；数据源 sample_file） */
    interface InterpretationLims {
      /** 是否找到该样本编号的样本信息；false = 没有 LIMS 数据（会阻止生成） */
      found: boolean | null;
      /** 命中的样本编号 */
      barcode: string | null;
      /** 患者编号 */
      patientId: string | null;
      /** 患者姓名 */
      patientName: string | null;
      /** 性别 */
      gender: string | null;
      /** 出生日期 */
      birthday: string | null;
      /** 年龄 */
      age: string | null;
      /** 录单癌种 */
      cancerType: string | null;
      /** 病理类型 */
      pathologicalType: string | null;
      /** 临床分期 */
      clinicalStage: string | null;
      /** 临床备注 */
      clinicalRemark: string | null;
      /** 医院/送检单位 */
      hospitalName: string | null;
      /** 送检医生 */
      doctorName: string | null;
      /** 样本类型 */
      specimenType: string | null;
      /** 样本量 */
      specimenQuantity: string | null;
      /** 样本来源 */
      sampleSource: string | null;
      /** 取材部位 */
      fromOrgan: string | null;
      /** 采样日期 */
      sampleCollectedAt: string | null;
      /** 收样日期 */
      sampleReceivedAt: string | null;
      /** 委托日期 */
      commissionedAt: string | null;
      /** 录单产品 */
      testingProgram: string | null;
      /** 样本备注 */
      sampleRemark: string | null;
      /** 实验室 */
      laboratoryName: string | null;
      /** 报告接收人 */
      reportReceiver: string | null;
      /** 送检邮箱 */
      emailAddress: string | null;
      /** 患者信息邮箱 */
      patientInfoEmail: string | null;
      /** 接诊医生邮箱 */
      doctorEmail: string | null;
    }

    /** 解读页 Tab② 集群对接：文件列表行（= 后端 InterpretationFileVo，data_file_status） */
    interface InterpretationFile {
      fileId: number;
      /** 文件类型：SNP / Indel / CNV / Fusion / CR_ALL / MSI / qc / Chem / Chemical_all */
      fileType: string | null;
      /** 数据类别：variant / druginfo … */
      dataType: string | null;
      /** 文件名 */
      fileName: string | null;
      /** 文件绝对路径（可复制） */
      filePath: string | null;
      /** 状态：Pending / Loaded / Error */
      status: string | null;
      /** 失败原因（status=Error 时） */
      message: string | null;
      /** 解析出的变异数 */
      mutNum: number | null;
      /** 分析日期（YYYYMMDD） */
      analysisDate: string | null;
      /** file_text 长度 */
      textLength: number | null;
      /** 更新时间 */
      updateTime: string | null;
    }

    /** 文件分页列表 */
    type InterpretationFileList = Common.PaginatingQueryRecord<InterpretationFile>;

    /**
     * 文件列表搜索参数：文件名 / 文件类型 / 状态 + 固定带上的 analysisId。
     * 管理列表（一个批次一个范围）不提供跨批次搜索，analysisId 由详情页传入。
     */
    type InterpretationFileSearchParams = CommonType.RecordNullable<
      Pick<InterpretationFile, 'fileName' | 'fileType' | 'status'> & Common.CommonSearchParams
    > & { analysisId: number; params?: Record<string, unknown> };

    /** 文件内容（= 后端 InterpretationFileContentVo） */
    interface InterpretationFileContent {
      fileId: number;
      fileName: string | null;
      fileType: string | null;
      /** 文件内容（超长时截断） */
      fileText: string | null;
      /** 是否被截断 */
      truncated: boolean | null;
      /** 原始长度 */
      textLength: number | null;
    }

    /**
     * 解读页 Tab③ 筛选位点：位点行
     * <p>
     * 后端按需求把明细表「除公共字段外的列全部返回」，返回的是 Map（key = **数据库列名**），
     * 所以这里不强约束字段：列的顺序/中文标题在前端 tab-variants.vue 的 COLUMN_SPEC 里维护。
     * 注意后端 Jackson 配了 non_null，值为 null 的列不会出现在 JSON 里 → 取值一律 `row[key] ?? '-'`。
     * 另外 is_reported 是 tinyint(1)，JDBC 会把它映射成布尔（true/false），判断报出状态要兼容两种。
     */
    type InterpretationVariant = Record<string, unknown>;

    /** 旧字段说明（保留注释便于对照表结构）：
     * （已改为 Map，下面这组字段仅作列语义参考，不再是接口契约） */
    interface InterpretationVariantLegacy {
      /** 位点主键（各明细表 id） */
      sourceId: number;
      /** SNP_INDEL / CNV / FUSION / CR_ALL */
      sourceType: string;
      /** 来源文件ID */
      fileId: number | null;
      gene: string | null;
      variant: string | null;
      oriVariant: string | null;
      /** 是否入报告：0 否 / 1 是 */
      isReported: number | null;
      filteredRationale: string | null;
      reviewedAt: string | null;
      // SNP/Indel
      transcript: string | null;
      exon: string | null;
      mutationType: string | null;
      chromosome: string | null;
      position: string | null;
      /** 合子状态 */
      homHet: string | null;
      /** 突变丰度(%) */
      mutFreq: string | null;
      mutDepth: string | null;
      totalDepth: string | null;
      // CNV
      copyNum: string | null;
      // Fusion
      gene1: string | null;
      gene2: string | null;
      /** DNA / RNA */
      tag: string | null;
      fusionReads: string | null;
      /** 检测结果（Fusion） */
      checkResult: string | null;
      // CR_ALL
      chgvs: string | null;
      phgvs: string | null;
      /** 知识库临床意义（CLNSIG 原文） */
      clinicalSignificance: string | null;
    }

    /** 位点分页列表 */
    type InterpretationVariantList = Common.PaginatingQueryRecord<InterpretationVariant>;

    /** 位点搜索参数：sourceType 必填（决定查哪张明细表）+ 基因 + 报出（1 是 / 0 否） */
    type InterpretationVariantSearchParams = CommonType.RecordNullable<{
      sourceType: string;
      gene: string;
      isReported: number;
      pageNum: number;
      pageSize: number;
    }> & { analysisId: number; params?: Record<string, unknown> };

    /** 报告预览：单条药物证据 */
    interface PreviewDrug {
      drugName: string | null;
      drugNameEn: string | null;
      disease: string | null;
      directTarget: string | null;
      evidenceType: string | null;
      evidenceRanking: string | null;
      evidencePhase: string | null;
      relationship: string | null;
      annotation: string | null;
      comment: string | null;
    }

    /** 报告预览：单个位点（体细胞或胚系） */
    interface PreviewVariant {
      sourceId: number;
      sourceType: string;
      gene: string | null;
      variant: string | null;
      oriVariant: string | null;
      mutationType: string | null;
      frequency: string | null;
      depth: string | null;
      zygosity: string | null;
      clinicalSignificance: number | null;
      clinicalSignificanceLabel: string | null;
      /** MATCHED / NOT_MATCHED */
      matchStatus: string | null;
      variationClass: string | null;
      parentMutationId: number | null;
      /** 文件里的原始判定（CLNSIG） */
      sourceClnsig: string | null;
      classificationLovd: string | null;
      drugMatch: PreviewDrug[];
    }

    /** 报告预览：一个分节 */
    interface PreviewSection {
      summary: { reportedCount?: number; matchedCount?: number; unmatchedCount?: number };
      items: PreviewVariant[];
    }

    /** 报告预览 JSON（= 后端 InterpretationPreviewVo，对齐设计书 7.6 契约） */
    interface InterpretationPreview {
      schemaVersion: string;
      templateCode: string | null;
      templateVersion: string | null;
      analysisId: number;
      reportId: number;
      reportInfo: Record<string, unknown>;
      sampleInfo: Record<string, unknown>;
      somaticVariants: PreviewSection;
      germlineVariants: PreviewSection;
      shengyuSomaticVariants: Record<string, unknown>[];
      shengyuGermlineVariants: Record<string, unknown>[];
      qualityControl: Record<string, unknown>;
      warnings: string[];
    }

    /** 解读页上下文（= 后端 InterpretationContextVo） */
    interface InterpretationContext {
      report: InterpretationReport;
      lims: InterpretationLims;
      /** 是否满足继续解读/生成的最低数据要求 */
      canGenerate: boolean | null;
      /** 阻止生成的错误 */
      errors: string[];
      /** 不阻断流程的告警 */
      warnings: string[];
    }
  }
}
