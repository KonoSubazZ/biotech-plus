/**
 * 样本信息类型定义
 * <p>
 * 放 src/typings/api/ 下（一个模块一个文件），命名空间 Api.<模块>.<实体>，
 * 与 api.ts / index.vue / search / drawer / 分组表单共用。
 * 字段 = 生产库录单样本表 myapp_webcrmsample 的 122 列（见 script/sql/business/report.sql），
 * 列名规范化为小写下划线后转驼峰；每个字段的注释里写了源表列名。
 */
declare namespace Api {
  namespace Report {
    /** 列表项（= 后端 SampleInfoVo） */
    interface SampleInfo {
      id: number;
      /** 年龄 */
      age: string | null;
      /** 样本编号 */
      barcode: string | null;
      /** 床位 */
      bed: string | null;
      /** 出生日期 */
      birthDay: string | null;
      birthplace: string | null;
      /** 录单癌种 */
      cancerType: string | null;
      /** 临床备注 */
      clinicalRemark: string | null;
      /** 临床分期 */
      clinicalStages: string | null;
      /** 收样日期 */
      collectDate: string | null;
      /** 客户名称 */
      customDesc: string | null;
      /** 客户 */
      customerName: string | null;
      /** 科室编码 */
      departmentCode: string | null;
      /** 医生姓名 */
      doctorName: string | null;
      /** 邮箱 */
      emailAddress: string | null;
      /** 委托日期 */
      enterDate: string | null;
      /** ERP销售 */
      erpSalerName: string | null;
      /** 录单产品 */
      erpTestName: string | null;
      /** 一级亲属患癌情况 */
      familyFirst: string | null;
      /** 一级亲属年龄 */
      familyFirstAge: string | null;
      /** 一级亲属癌种 */
      familyFirstCancerType: string | null;
      /** 一级亲属确诊时间 */
      familyFirstConfirmTime: string | null;
      /** 二级亲属患癌情况 */
      familySecond: string | null;
      /** 二级亲属年龄 */
      familySecondAge: string | null;
      /** 二级亲属癌种 */
      familySecondCancerType: string | null;
      /** 二级亲属确诊时间 */
      familySecondConfirmTime: string | null;
      /** 加急编号 */
      fastCode: string | null;
      /** 第一次治疗史 */
      firstTreatment: string | null;
      /** 取材部位 */
      fromOrgan: string | null;
      /** 基因检测结果 */
      geneResult: string | null;
      /** 基因型 */
      geneType: string | null;
      /** 取材日期 */
      getSpecDate: string | null;
      /** 文库编号 */
      libraryName: string | null;
      /** 病区 */
      locationName: string | null;
      /** 邮寄地址 */
      mailingAddress: string | null;
      /** 经理邮箱 */
      managerEmail: string | null;
      /** 订单编号 */
      orderCode: string | null;
      /** 门诊号 */
      outpatient: string | null;
      /** 病理类型 */
      pathologicalType: string | null;
      /** 病理号 */
      pathologyNum: string | null;
      /** 姓名 */
      patientName: string | null;
      /** 患者电话 */
      patientPhone: string | null;
      /** 收款金额 */
      payAmount: string | null;
      /** 患者编号 */
      pcode: string | null;
      /** 项目经理邮箱 */
      pmEmail: string | null;
      /** 报告接收人电话 */
      receiverTelephone: string | null;
      /** 录单人 */
      recorder: string | null;
      /** 录单人编码 */
      recorderCode: string | null;
      /** 报告接收人 */
      reportReceiver: string | null;
      /** 房间 */
      room: string | null;
      /** 销售邮箱(ERP) */
      salerEmail: string | null;
      /** 样本备注 */
      sampleRemark: string | null;
      /** 样本来源 */
      sampleSource: string | null;
      /** 采样日期 */
      sampleTime: string | null;
      /** 样本类型 */
      sampleType: string | null;
      /** 第二次治疗史 */
      secondTreatment: string | null;
      /** 寄出日期 */
      sendDate: string | null;
      /** 性别 */
      sex: string | null;
      /** 标本编号 */
      specimenNo: string | null;
      /** 样本数量 */
      specimenNum: string | null;
      /** 支持邮箱 */
      supportEmail: string | null;
      /** 第三次治疗史 */
      thirdTreatment: string | null;
      /** 单位 */
      unit: string | null;
      /** 接诊医生邮箱 */
      admissionDoctorEmail: string | null;
      /** 接诊医生电话 */
      admissionDoctorPhone: string | null;
      /** 就诊医院 */
      admissionHospital: string | null;
      /** 癌种1 */
      cancerType1: string | null;
      /** 癌种编码 */
      cancerTypeCode: string | null;
      /** 合同名称 */
      contractName: string | null;
      /** 合同编号 */
      contractsNo: string | null;
      /** 单位名称 */
      corpDesc: string | null;
      /** 单位编号 */
      corpNo: string | null;
      /** 客户类型 */
      customerType: string | null;
      /** 检测方法 */
      detectionMethod: string | null;
      /** 检测时间 */
      detectionTime: string | null;
      /** 快递公司 */
      expressName: string | null;
      /** 快递单号 */
      expressNo: string | null;
      /** 二级亲属患癌情况 */
      familyKinshipTwoCancer: string | null;
      /** 第一次治疗用药方案 */
      firstTreatmentDrugRegimen: string | null;
      /** 第一次治疗时长 */
      firstTreatmentDuration: string | null;
      /** 第一次治疗疗效 */
      firstTreatmentEffect: string | null;
      /** 第一次治疗方式 */
      firstTreatmentMethod: string | null;
      /** 第一次治疗时间 */
      firstTreatmentTime: string | null;
      /** 实验室 */
      laboratoryName: string | null;
      /** 运营负责人编码 */
      operateManagerCode: string | null;
      /** 运营负责人 */
      operateManagerDesc: string | null;
      /** 运营负责人邮箱 */
      operateManagerEmail: string | null;
      /** 订单金额 */
      orderMoney: string | null;
      /** 其它附件 */
      otherAttachments: string | null;
      /** 病理报告 */
      pathologyReport: string | null;
      /** 患者信息邮箱 */
      patientInfoEmail: string | null;
      /** 患者是否肿瘤 */
      patientInfoIsCancer: string | null;
      /** 收款完成日期 */
      payFinishDate: string | null;
      /** 录单单位 */
      recorderDesc: string | null;
      /** 销售 */
      salesMan: string | null;
      /** 销售编码 */
      salesManCode: string | null;
      /** 销售邮箱 */
      salesManEmail: string | null;
      /** 送样地址 */
      sampleAddress: string | null;
      /** 送样联系人 */
      sampleContactDesc: string | null;
      /** 送样联系电话 */
      sampleContactPhone: string | null;
      /** 数量单位 */
      sampleNumUnit: string | null;
      /** 产品编码 */
      sampleProductCode: string | null;
      /** 第二次治疗用药方案 */
      secondTreatmentDrugRegimen: string | null;
      /** 第二次治疗时长 */
      secondTreatmentDuration: string | null;
      /** 第二次治疗疗效 */
      secondTreatmentEffect: string | null;
      /** 第二次治疗方式 */
      secondTreatmentMethod: string | null;
      /** 第二次治疗时间 */
      secondTreatmentTime: string | null;
      /** 流水号 */
      serialNumber: string | null;
      /** 具体癌种 */
      specificCancer: string | null;
      /** 送检机构 */
      testingInstitution: string | null;
      /** 第三次治疗用药方案 */
      thirdTreatmentDrugRegimen: string | null;
      /** 第三次治疗时长 */
      thirdTreatmentDuration: string | null;
      /** 第三次治疗疗效 */
      thirdTreatmentEffect: string | null;
      /** 第三次治疗方式 */
      thirdTreatmentMethod: string | null;
      /** 第三次治疗时间 */
      thirdTreatmentTime: string | null;
      /** 科室名称 */
      departmentDesc: string | null;
      /** 异常备注 */
      abnormalRemark: string | null;
      /** 结算渠道 */
      checkoutLogic: string | null;
      /** 到样日期 */
      dyDate: string | null;
      /** 账期(天) */
      paymentDate: string | null;
      /** 样本属性 */
      sampleAttribute: string | null;
      /** 签约时间 */
      signTime: string | null;
      /** 冻结状态 */
      block: string | null;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type SampleInfoList = Common.PaginatingQueryRecord<SampleInfo>;

    /**
     * 搜索参数
     * <p>
     * 搜索栏 6 项 → 字段：样本编号 barcode、姓名 patientName、客户 customerName、
     * 录单癌种 cancerType、录单产品 erpTestName、委托日期走 params.beginTime / params.endTime。
     */
    type SampleInfoSearchParams = CommonType.RecordNullable<
      Pick<SampleInfo, 'barcode' | 'patientName' | 'customerName' | 'cancerType' | 'erpTestName'> &
        Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 SampleInfoBo） */
    interface SampleInfoForm {
      id?: number | null;
      age?: string | null;
      barcode?: string | null;
      bed?: string | null;
      birthDay?: string | null;
      birthplace?: string | null;
      cancerType?: string | null;
      clinicalRemark?: string | null;
      clinicalStages?: string | null;
      collectDate?: string | null;
      customDesc?: string | null;
      customerName?: string | null;
      departmentCode?: string | null;
      doctorName?: string | null;
      emailAddress?: string | null;
      enterDate?: string | null;
      erpSalerName?: string | null;
      erpTestName?: string | null;
      familyFirst?: string | null;
      familyFirstAge?: string | null;
      familyFirstCancerType?: string | null;
      familyFirstConfirmTime?: string | null;
      familySecond?: string | null;
      familySecondAge?: string | null;
      familySecondCancerType?: string | null;
      familySecondConfirmTime?: string | null;
      fastCode?: string | null;
      firstTreatment?: string | null;
      fromOrgan?: string | null;
      geneResult?: string | null;
      geneType?: string | null;
      getSpecDate?: string | null;
      libraryName?: string | null;
      locationName?: string | null;
      mailingAddress?: string | null;
      managerEmail?: string | null;
      orderCode?: string | null;
      outpatient?: string | null;
      pathologicalType?: string | null;
      pathologyNum?: string | null;
      patientName?: string | null;
      patientPhone?: string | null;
      payAmount?: string | null;
      pcode?: string | null;
      pmEmail?: string | null;
      receiverTelephone?: string | null;
      recorder?: string | null;
      recorderCode?: string | null;
      reportReceiver?: string | null;
      room?: string | null;
      salerEmail?: string | null;
      sampleRemark?: string | null;
      sampleSource?: string | null;
      sampleTime?: string | null;
      sampleType?: string | null;
      secondTreatment?: string | null;
      sendDate?: string | null;
      sex?: string | null;
      specimenNo?: string | null;
      specimenNum?: string | null;
      supportEmail?: string | null;
      thirdTreatment?: string | null;
      unit?: string | null;
      admissionDoctorEmail?: string | null;
      admissionDoctorPhone?: string | null;
      admissionHospital?: string | null;
      cancerType1?: string | null;
      cancerTypeCode?: string | null;
      contractName?: string | null;
      contractsNo?: string | null;
      corpDesc?: string | null;
      corpNo?: string | null;
      customerType?: string | null;
      detectionMethod?: string | null;
      detectionTime?: string | null;
      expressName?: string | null;
      expressNo?: string | null;
      familyKinshipTwoCancer?: string | null;
      firstTreatmentDrugRegimen?: string | null;
      firstTreatmentDuration?: string | null;
      firstTreatmentEffect?: string | null;
      firstTreatmentMethod?: string | null;
      firstTreatmentTime?: string | null;
      laboratoryName?: string | null;
      operateManagerCode?: string | null;
      operateManagerDesc?: string | null;
      operateManagerEmail?: string | null;
      orderMoney?: string | null;
      otherAttachments?: string | null;
      pathologyReport?: string | null;
      patientInfoEmail?: string | null;
      patientInfoIsCancer?: string | null;
      payFinishDate?: string | null;
      recorderDesc?: string | null;
      salesMan?: string | null;
      salesManCode?: string | null;
      salesManEmail?: string | null;
      sampleAddress?: string | null;
      sampleContactDesc?: string | null;
      sampleContactPhone?: string | null;
      sampleNumUnit?: string | null;
      sampleProductCode?: string | null;
      secondTreatmentDrugRegimen?: string | null;
      secondTreatmentDuration?: string | null;
      secondTreatmentEffect?: string | null;
      secondTreatmentMethod?: string | null;
      secondTreatmentTime?: string | null;
      serialNumber?: string | null;
      specificCancer?: string | null;
      testingInstitution?: string | null;
      thirdTreatmentDrugRegimen?: string | null;
      thirdTreatmentDuration?: string | null;
      thirdTreatmentEffect?: string | null;
      thirdTreatmentMethod?: string | null;
      thirdTreatmentTime?: string | null;
      departmentDesc?: string | null;
      abnormalRemark?: string | null;
      checkoutLogic?: string | null;
      dyDate?: string | null;
      paymentDate?: string | null;
      sampleAttribute?: string | null;
      signTime?: string | null;
      block?: string | null;
    }

    /** Excel 导入结果（= 后端 SampleInfoImportResultVo） */
    interface SampleInfoImportResult {
      /** 解析到的数据行数（不含表头） */
      total: number;
      /** 新增条数 */
      inserted: number;
      /** 更新条数（样本编号已存在，按文件覆盖） */
      updated: number;
      /** 失败条数 */
      failed: number;
      /** 每行失败原因（含行号） */
      errors: string[];
    }

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
