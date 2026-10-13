/**
 * 质控标准类型定义
 * <p>
 * 放 src/typings/api/ 下（一个模块一个文件），命名空间 Api.<模块>.<实体>，
 * 与 api.ts / index.vue / search / drawer 共用。不要写成多份或就地声明。
 * 字段与 table qc_standard 一一对应（见 script/sql/business/qc.sql）。
 */
declare namespace Api {
  namespace Qc {
    /** 列表项（= 后端 QcStandardVo） */
    interface QcStandard {
      id: number;
      /** 关联产品（product_config.id） */
      productId: number;
      /** 质控项目名称（中文展示名；与 qc_record.qc_item 对应） */
      qcItem: string;
      /** 质控参数（英文原始字段名；判定时与生信质控实际值比对，空值不参与判定） */
      qcParam?: string | null;
      /** 质控类别：wet_lab 湿实验 / bioinfo 生信 */
      qcCategory: string;
      /** 合格下限；NULL 表示不设下限。用字符串以支持 "<0.5" 这类非数值写法 */
      minValue?: string | null;
      /** 合格上限；NULL 表示不设上限 */
      maxValue?: string | null;
      /** 单位（%、X 等） */
      unit?: string | null;
      /** 状态：active 启用 / inactive 停用 */
      status: string;
      remark?: string | null;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type QcStandardList = Common.PaginatingQueryRecord<QcStandard>;

    /** 搜索参数（与项目其它模块保持同一写法） */
    type QcStandardSearchParams = CommonType.RecordNullable<
      Pick<QcStandard, 'productId' | 'qcItem' | 'qcCategory' | 'status'> & Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 QcStandardBo） */
    interface QcStandardForm {
      id?: number | null;
      /** 关联产品（必填，表单里校验；初次打开抽屉为 null） */
      productId?: number | null;
      qcItem: string;
      /** 质控参数（英文原始字段名，判定用） */
      qcParam?: string | null;
      qcCategory: string;
      minValue?: string | null;
      maxValue?: string | null;
      unit?: string | null;
      status?: string | null;
      remark?: string | null;
    }

    /**
     * 湿实验质控（= 后端 WetLabQcVo）
     * <p>
     * **宽表：一行一个样本，每个质控项一列**（形态对齐生信质控 BioinfoQc）。
     * 列名与后端列一致 = 质控标准的 qc_param（英文原始字段名），中文名在 qc_standard.qc_item 里；
     * 阈值（上下限/单位）留在标准表，本表只存实测值。表见 script/sql/business/wet-lab-qc.sql。
     */
    interface WetLabQc {
      id: number;
      /** 样本条码（sample_file.subbarcode） */
      subbarcode: string;
      /** 肿瘤纯度（列名 patho_tumor_purity） */
      pathoTumorPurity?: string | null;
      /** DNA总量（ng）（列名 dna_total_ng） */
      dnaTotalNg?: string | null;
      /** 检测结果判断（等级 A/B/C/D）（列名 dna_degradation） */
      dnaDegradation?: string | null;
      /** 预文库总量（ng）（列名 pre_lib_total_ng） */
      preLibTotalNg?: string | null;
      /** 终文库浓度（ng/uL）（列名 final_lib_conc） */
      finalLibConc?: string | null;
      /** DNA总量（ng）对照（列名 dna_total_ng_normal） */
      dnaTotalNgNormal?: string | null;
      /** 预文库总量（ng）对照（列名 pre_lib_total_ng_normal） */
      preLibTotalNgNormal?: string | null;
      /** 终文库浓度（ng/uL）对照（列名 final_lib_conc_normal） */
      finalLibConcNormal?: string | null;
      /** 质控状态：passed 合格 / failed 不合格 / pending 待判定（服务端按 qc_standard 阈值判定） */
      status?: string | null;
      /** 通知发送状态（已生成（文档）/ 已发送（邮件））—— 暂不在页面展示 */
      notifyStatus?: string | null;
      createTime?: string | null;
    }

    /** 湿实验质控分页列表 */
    type WetLabQcList = Common.PaginatingQueryRecord<WetLabQc>;

    /** 湿实验质控搜索参数（样本条码 模糊，质控状态 等值；接口推送的行没有别的分类字段可筛） */
    type WetLabQcSearchParams = CommonType.RecordNullable<
      Pick<WetLabQc, 'subbarcode' | 'status'> & Common.CommonSearchParams
    >;

    /**
     * 修改表单（= 后端 WetLabQcBo）
     * <p>
     * subbarcode 只展示不可改（它是接口 upsert 的业务键）；status 由后端按阈值重判；
     * notifyStatus 是服务端记的通知状态，不进表单。
     */
    type WetLabQcForm = Omit<WetLabQc, 'subbarcode' | 'status' | 'notifyStatus' | 'createTime'> & {
      id: number;
      /** 样本条码：只展示不可改（抽屉里 disabled），所以允许 null */
      subbarcode: string | null;
    };

    /**
     * 生信质控（= 后端 BioinfoQcVo）
     * <p>
     * 注意：decimal 类型字段后端统一按**字符串**下发（BigDecimal 序列化成 String，避免 JS 精度丢失），
     * 例如 `rawBase: "9.99"`；bigint 字段（targetSize/totalReads/mappedReads/beforeDuplicate）仍是 number。
     * <p>
     * 宽表：一行一个样本，字段与表 bioinfo_qc 一一对应（script/sql/business/bioinfo-qc.sql）。
     * 字段暂无中文名，页面按字段名展示。数据由接口灌入（POST /qc/bioinfo/receive），页面只读。
     */
    interface BioinfoQc {
      id: number;
      /** 样本编号（= sample_file.subbarcode）；接口按它解析归属药企 */
      subbarcode: string | null;
      /** 上游记录 id（幂等/追溯用，页面不展示） */
      sourceId?: string | null;
      /** 质控状态：passed 合格 / failed 不合格 / pending 待判定（按 qc_standard 阈值判定） */
      status: string | null;
      /** 通知发送状态（已生成（文档）/ 已发送（邮件））—— 暂不在页面展示 */
      notifyStatus?: string | null;
      bam: string | null;
      gender: string | null;
      libId: string | null;
      paired: string | null;
      rawGc: string | null;
      hotPos: string | null;
      rawQ20: string | null;
      rawQ30: string | null;
      pipeline: string | null;
      rawBase: string | null;
      sshPath: string | null;
      duplicate: string | null;
      peakSize: string | null;
      qcResult: string | null;
      sqi16: string | null;
      bioResult: string | null;
      peakRange: string | null;
      concordance: string | null;
      chrall: string | null;
      librarySize: string | null;
      bamRealign: string | null;
      targetSize: number | null;
      totalReads: number | null;
      mappingRate: string | null;
      averageDepth: string | null;
      coverageTarget: string | null;
      mappedReads: number | null;
      duplicateTarget: string | null;
      contaminationLevel: string | null;
      coverageTarget50x: string | null;
      probeCaptureRatio: string | null;
      coverageTarget500x: string | null;
      coverageTarget1000x: string | null;
      coverageUniformity02xadOnTarget: string | null;
      beforeDuplicate: number | null;
      createTime?: string | null;
    }

    /** 生信质控分页列表 */
    type BioinfoQcList = Common.PaginatingQueryRecord<BioinfoQc>;

    /** 生信质控搜索参数（样本编号 / 文库编号 模糊，状态 等值） */
    type BioinfoQcSearchParams = CommonType.RecordNullable<
      Pick<BioinfoQc, 'subbarcode' | 'libId' | 'status'> & Common.CommonSearchParams
    >;

    /** 样本编号 → 药企解析结果（= 后端 BioinfoQcTenantVo） */
    interface BioinfoQcTenant {
      subbarcode: string | null;
      /** sample_file.customer_name（= 药企名） */
      customerName: string | null;
      /** 解析出的药企编号 */
      tenantId: string | null;
      /** false = 样本查不到，或客户名匹配不到启用中的药企 */
      found: boolean | null;
    }

    /**
     * 修改表单（= 后端 BioinfoQcBo）
     * <p>
     * subbarcode 只展示不可改（它是接口 upsert 的业务键）；status 由后端按阈值重判；
     * sourceId / createTime 不入表单。
     */
    type BioinfoQcForm = Omit<BioinfoQc, 'sourceId' | 'status' | 'createTime'> & {
      id: number;
    };

    /**
     * 质控不合格通知 处理表单（= 后端 QcFailedNotifyBo）
     * <p>
     * 勾选项传 boolean：页面用多选框，落库时服务端转中文名，渲染时模板用 box() 出 ☑/□。
     */
    interface NotifyForm {
      /** 样本编号（后面所有回填都按它查 sample_file） */
      subbarcode: string | null;
      /** 质控类型（wet_lab 湿实验 / bioinfo 生信）—— 在哪个质控页点处理；落库区分通知来源 */
      qcCategory: string | null;
      /** 研究方案编号 */
      studyNo: string | null;
      /** 诺禾项目编号 */
      projectNo: string | null;
      /** 试验参与者编号 */
      participantNo: string | null;
      /** 研究中心名称 */
      center: string | null;
      /** 样本接收日期 */
      receiveDate: string | null;
      /** 样本类型 */
      sampleType: string | null;
      /** 样本数量 */
      sampleCount: string | null;
      /** 检测产品 */
      product: string | null;
      /** 样本质控环节 */
      stageReceive: boolean;
      stagePathology: boolean;
      stageNucleic: boolean;
      stageLibrary: boolean;
      stageSequencing: boolean;
      /** 不合格原因（默认带出按质控标准算出的不达标项，可改；会落库并渲染进文档） */
      reason: string | null;
      /** 是否终止检测 */
      terminateYes: boolean;
      terminatePause: boolean;
      /** 建议处理办法 */
      remedyResample: boolean;
      remedyWaxBlock: boolean;
      remedyMoreSlices: boolean;
      remedyResend: boolean;
      remedyOther: boolean;
      remedyOtherText: string | null;
      /** 反馈与复核 */
      reporter: string | null;
      reportTime: string | null;
      reviewer: string | null;
      reviewTime: string | null;
    }

    /**
     * 处理弹窗预填（= 后端 QcFailedNotifyPrefillVo）
     * <p>
     * 表单字段先按 sample_file 回填、再用最近一条生成记录覆盖；generated=true 表示已生成过。
     */
    interface NotifyPrefill extends NotifyForm {
      /** sample_file.id（一定有） */
      sampleFileId: number | null;
      /** 生信质控 id */
      bioinfoQcId: number | null;
      /** 湿实验质控 id（湿实验宽表未建，先为 null） */
      wetLabQcId: number | null;
      /** 分析时间（质控记录入库时间） */
      analysisTime: string | null;
      /** 客户（= 药企名） */
      customerName: string | null;
      /** 上次生成的记录 id（下载用） */
      lastNotifyId: number | null;
      /** 上次的通知单号 */
      notifyNo: string | null;
      /** 上次生成时间 */
      lastGeneratedAt: string | null;
      /** 上次生成的 DOCX 文件名 */
      lastDocxName: string | null;
      /** 上次生成的 PDF 路径；有值表示已转出 PDF，下载优先给 PDF */
      lastPdfName: string | null;
      /** 上上签合同号（送签后才有；有值表示可「刷新签署状态」） */
      signContractId: string | null;
      /** 签名后 PDF 路径（签署完成并拉取后才有；有值表示可下载签名件） */
      signedPdfPath: string | null;
      /** 通知状态：已生成（文档）/ 已发送（邮件） */
      notifyStatus: string | null;
      /** 是否已经生成过 */
      generated: boolean | null;
    }

    /** 生成结果（= 后端 QcFailedNotifyResultVo） */
    interface NotifyResult {
      notifyId: number;
      notifyNo: string;
      jsonPath: string;
      docxName: string;
      docxPath: string;
      outputBytes: number | null;
      sha256: string | null;
      elapsedMs: number | null;
      rendererLog: string | null;
      /** 生成的 PDF 路径；转换失败时为空 */
      pdfPath: string | null;
      /** 生成的 PDF 文件名；转换失败时为空 */
      pdfName: string | null;
    }

    /**
     * 质控通知送签结果（= 后端 QcFailedNotifySignVo）
     * <p>
     * 「送签」与「刷新签署状态」共用：送签返回合同号；刷新返回网关状态，
     * signed=true 表示已签完、后端已把签名件拉到 notify/signed/（signedPdfPath/Name 有值）。
     */
    interface NotifySignResult {
      /** 通知记录 id */
      notifyId: string;
      /** 上上签合同号（送签后回填；刷新时原样带回） */
      contractId: string | null;
      /** 网关返回的签署状态描述（原样） */
      contractStatus: string | null;
      /** 是否已签署完成（后端按「完成词」判断） */
      signed: boolean;
      /** 签名后 PDF 路径（已签署时才有） */
      signedPdfPath: string | null;
      /** 签名后 PDF 文件名（已签署时才有） */
      signedPdfName: string | null;
      /** 回写后的通知状态：已送签 / 已签署 等 */
      notifyStatus: string | null;
    }
  }
}
