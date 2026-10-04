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

    /** 改靶候选：NKB 位点节点（父级由知识库查到） */
    interface NkbVariantNode {
      /** NKB 节点ID（gene_variant_id） */
      mutationId: number | null;
      gene: string | null;
      /** 节点名（V559D / Exon11 Mutation / Active Mutation） */
      variantName: string | null;
      /** 功能判定：激活/失活/未知/无影响 */
      effectText: string | null;
    }

    /** 临床试验证据（第二类证据；对齐实际报告「临床试验信息」表） */
    interface PreviewTrial {
      /** 试验登记号（NCTxxxxxxxx） */
      trialId: string | null;
      /** 临床试验名称 */
      title: string | null;
      /** 肿瘤类型 */
      trialCondition: string | null;
      /** 阶段原值（Phase II） */
      phase: string | null;
      /** 阶段中文（II期） */
      phaseText: string | null;
      /** 地点 */
      location: string | null;
      /** 对应药物 */
      drugName: string | null;
      /** 证据注释ID */
      annotationId: number | null;
    }

    /** 报告预览：单条药物证据（对齐 report_en7 的 evidence 行） */
    interface PreviewDrug {
      annotationId: number | null;
      drugId: number | null;
      /** 命中的知识库节点ID */
      mutationId: number | null;
      /** 命中的节点名（V559D / Exon11 Mutation / Active Mutation / Inactive Mutation） */
      nodeName: string | null;
      /** 是否来自「其他癌种获批药」（en7 会降格为 C 级） */
      fromOtherCancer: boolean | null;
      drugName: string | null;
      drugNameEn: string | null;
      diseaseId: number | null;
      disease: string | null;
      relationship: string | null;
      /** 关系字典ID：1 敏感性增加 / 4 有益的 / 6 抗药性（判断耐药用它） */
      relationshipId: number | null;
      /** BENEFIT / RESISTANT */
      relation: string | null;
      directTarget: string | null;
      evidenceType: string | null;
      evidencePhase: string | null;
      evidencePhaseId: number | null;
      evidenceRanking: string | null;
      /** 等级码 1-8（获益A-D / 耐药A-D），9 = 其他（不输出） */
      approveRange: number | null;
      /** 等级名 A/B/C/D */
      levelName: string | null;
      give: string | null;
      otherTestRequired: string | null;
      /** 既往是否有临床结果（Y / null / N；en7 getGive 用它判 1 或 0） */
      hasPreviousClinicalResult: string | null;
      /** 获批机构（approved_drug.approving_agency） */
      approvingAgency: string | null;
      /** 获批上市(24)说明：approved_drug_evw.approval_description_chinese */
      approvalDescription: string | null;
      /** 指南推荐(23)说明：按 NCCN/CSCO 拼好的中文串 */
      guidelineDescription: string | null;
      /** 指南类型：NCCN / CSCO */
      guidelineTypes: string[] | null;
      /** 说明（annotation_chinese），无指南/批准说明时界面显示它 */
      annotation: string | null;
      /** NKB 内部维护备注（变更记录）——不展示、后端已不再取数，仅兼容历史冻结 JSON */
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
      /** 人工改靶的父级节点ID（可多个；空 = 未改靶） */
      parentMutationIds: number[] | null;
      /** 人工改靶父级的节点名（与 parentMutationIds 同序，取不到为 null） */
      parentMutationNames: (string | null)[] | null;
      /** 文件里的原始判定（CLNSIG） */
      sourceClnsig: string | null;
      classificationLovd: string | null;
      /** 证据明细（全量保留：药物 × 等级 × 证据癌种） */
      drugMatch: PreviewDrug[];
      /** 临床试验证据（招募中/邀请入组；ID/名称/肿瘤类型/阶段/药物/地点） */
      trials: PreviewTrial[] | null;
      /** 按等级分组的药物名串（drugsA..D / resistantDrugsA..D），去重键=药名+癌种 */
      drugGroups: Record<string, string | null> | null;
      /** 审核列表（{drug, relation, level, evidenceDiseaseId, evidenceDiseaseName, matchedNode}） */
      drugAuditList: Record<string, unknown>[] | null;
      /** 是否在知识库中命中节点 */
      inNkb: boolean | null;
      /** 命中的知识库节点名（父级时能看到 Active Mutation 等） */
      matchedNode: string | null;
      /** 关联突变节点名：命中节点自身 + 一层父级（未收录时为空，`基因 位点` 由前端追加） */
      relatedMutations: string[] | null;
      /** 知识库功能判定：激活/失活/未知/无影响 */
      effectText: string | null;
      /** 位点用药说明（失活/扩增/缺失/未明四段模板） */
      description: string | null;
      /** 来源文件类型（SNP/Indel/CNV/Fusion/CR_ALL） */
      fileType: string | null;
      /** 核酸类型：DNA / RNA（只有融合行有值） */
      nucleicAcid: string | null;
      /** 类型列展示值：体系|变异类别|核酸类型（如 S|Indel 、S|Somatic|RNA） */
      typeText: string | null;
      /** 变异类型展示值（VEP ExonicFunc 中文：错义突变/移码突变…） */
      variantTypeText: string | null;
      /** 丰度/reads 展示值：DNA → 45.47%；RNA 融合 → reads 数（无单位） */
      abundanceText: string | null;
      /** 命中的知识库节点ID（自身或父级） */
      matchedMutationId: number | null;
      /** 基因说明（NKB gene_description，Approved） */
      geneDescription: string | null;
      /** 信号通路说明（NKB gene_description.pathway_description_chinese，与基因说明同一行） */
      pathwayDescription: string | null;
      /** 位点说明（NKB gene_variant_description，按命中的节点） */
      variantDescription: string | null;
      /** 突变说明（HGVS → 中文，移植 en7 的 translate_hgvs.pl） */
      mutationExplanation: string | null;
    }

    /** 报告预览：一个分节 */
    interface PreviewSection {
      summary: { reportedCount?: number; matchedCount?: number; unmatchedCount?: number };
      items: PreviewVariant[];
      /** 分节的癌种上下文（公共组装器写入） */
      analysisId?: number | null;
      reportId?: number | null;
      diseaseId?: number | null;
      diseaseName?: string | null;
      gender?: string | null;
      /** 同输入同 JSON：本仓不写时间，固定 null */
      matchedAt?: string | null;
    }

    /**
     * 报告 JSON（= 后端 ReportTemplateData，schemaVersion 1.2）
     * <p>
     * 公共字段（reportInfo / sampleInfo / 两个位点分节 / 开关 / warnings）由后端公共组装器写；
     * 模板专属字段由 report_template.module_code 驱动的 Handler 挂到**顶层**
     * （shengyuSomaticVariants / shengyuGermlineVariants / qualityControl），没有 modules.* 包装层。
     */
    interface InterpretationPreview {
      schemaVersion: string;
      templateCode: string | null;
      templateVersion: string | null;
      analysisId: number;
      reportId: number;
      reportInfo: Record<string, unknown>;
      showTissueBloodSample?: boolean;
      showBloodOnlySample?: boolean;
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
