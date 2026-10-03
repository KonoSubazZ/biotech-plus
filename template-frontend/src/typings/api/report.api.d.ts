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
  }
}
