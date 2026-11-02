/**
 * 样本信息类型定义（cms_sample，药企 CMS 接口字段 + 公共字段）
 * <p>
 * 只包含 85 个 biotech_* 有效字段；59 个 deprecated_* 兼容列不在前端类型里。
 */
declare namespace Api {
  namespace Report {
    interface CmsSample {
      id: number;
      /** CMS订单批次主键 */
      cmsBatchId: string | null;
      /** 正式订单编号 */
      orderNo: string | null;
      /** 诺禾编号 */
      subbarcode: string | null;
      /** 订单批次编号 */
      batchNo: string | null;
      /** 订单业务类型 */
      businessType: string | null;
      /** 订单收样确认状态 */
      receiveConfirmStatus: string | null;
      /** 合同甲方客户编码 */
      customerCode: string | null;
      /** 正式订单客户名称 */
      customerName: string | null;
      /** 客户类型 */
      customerType: string | null;
      /** 关联合同编号 */
      contractNo: string | null;
      /** 关联合同名称 */
      contractName: string | null;
      /** 研究方案编号 */
      schemeNo: string | null;
      /** 药企项目编号 */
      projectNo: string | null;
      /** 研究中心名称 */
      researchCenter: string | null;
      /** 合同事业部编码 */
      departmentCode: string | null;
      /** 合同签订日期 */
      contractSignDate: string | null;
      /** 合同账期 */
      paymentTerm: string | null;
      /** 合同结算逻辑 */
      checkoutLogic: string | null;
      /** 合同冻结状态 */
      contractBlockStatus: string | null;
      /** 合同联系人邮箱 */
      contractContactEmail: string | null;
      /** 合同运营经理邮箱 */
      operatorEmail: string | null;
      /** 合同甲方CSS主账号邮箱 */
      customerAccountEmail: string | null;
      /** 报告接收人 */
      reportReceiver: string | null;
      /** 报告接收邮箱 */
      reportEmail: string | null;
      /** 报告接收人电话 */
      reportReceiverPhone: string | null;
      /** 报告邮寄地址 */
      reportMailingAddress: string | null;
      /** 运营经理名称 */
      operatorName: string | null;
      /** 运营经理编码 */
      operatorCode: string | null;
      /** 合同跟进销售名称 */
      salesName: string | null;
      /** 合同跟进销售编码 */
      salesCode: string | null;
      /** 合同关联销售邮箱 */
      salesEmail: string | null;
      /** 订单保存的销售名称 */
      orderSalesName: string | null;
      /** 合同技术支持邮箱 */
      supportEmail: string | null;
      /** 合同经理邮箱 */
      managerEmail: string | null;
      /** 订单创建人编码 */
      orderRecorderCode: string | null;
      /** 订单创建人名称 */
      orderRecorderName: string | null;
      /** 样本记录人名称 */
      sampleRecorderName: string | null;
      /** 订单销售渠道 */
      salesChannel: string | null;
      /** 订单销售区域 */
      salesRegion: string | null;
      /** 送检人 */
      senderName: string | null;
      /** 送检人联系电话 */
      senderPhone: string | null;
      /** 送检日期 */
      sendDate: string | null;
      /** 样本快递公司 */
      expressCompany: string | null;
      /** 样本快递单号 */
      expressNo: string | null;
      /** 运输条件 */
      transportCondition: string | null;
      /** 运输条件编码 */
      transportConditionCode: string | null;
      /** 是否带温控计 */
      hasTemperatureLogger: string | null;
      /** 收样实验室 */
      receivingLab: string | null;
      /** 收样联系人 */
      receivingContact: string | null;
      /** 收样联系电话 */
      receivingPhone: string | null;
      /** 收样地址 */
      receivingAddress: string | null;
      /** 客户采样识别码 */
      customerSamplingCode: string | null;
      /** 样本名称 */
      sampleName: string | null;
      /** 试验参与者编号 */
      subjectNo: string | null;
      /** 性别 */
      sex: string | null;
      /** 出生年份 */
      birthYear: string | null;
      /** 临床诊断或疾病类型 */
      diseaseType: string | null;
      /** 访视周期 */
      visitCycle: string | null;
      /** 产品编码 */
      productCode: string | null;
      /** 产品名称 */
      productName: string | null;
      /** 样本类型 */
      sampleType: string | null;
      /** 样本类型家族码 */
      sampleTypeCode: string | null;
      /** 样本数量 */
      sampleQuantity: string | null;
      /** 数量单位 */
      quantityUnit: string | null;
      /** 样本属性 */
      sampleAttribute: string | null;
      /** 取材部位 */
      collectionSite: string | null;
      /** 组织类型 */
      tissueType: string | null;
      /** 肿瘤类型 */
      tumorType: string | null;
      /** 原发部位 */
      primaryTumorSite: string | null;
      /** 采集日期 */
      collectionDate: string | null;
      /** 检测方式 */
      assayMethod: string | null;
      /** 样本备注 */
      remark: string | null;
      /** 异常备注 */
      abnormalRemark: string | null;
      /** 管控类型 */
      controlType: string | null;
      /** 病理编号 */
      pathologyNo: string | null;
      /** 04类附件路径 */
      pathologyReportPaths: string | null;
      /** 03类附件路径 */
      otherAttachmentPaths: string | null;
      /** 订单收样确认时间 */
      receiveConfirmTime: string | null;
      /** 订单提交时间 */
      orderSubmitTime: string | null;
      /** 到达日期 */
      arrivalDate: string | null;
      /** 到达时间 */
      arrivalTime: string | null;
      /** 实际支付金额 */
      paidAmount: string | null;
      /** 订单金额 */
      orderAmount: string | null;
      /** 订单流水标识 */
      paymentSerialNo: string | null;
      /** 付款完成时间 */
      paymentFinishTime: string | null;
      createTime?: string | null;
    }

    type CmsSampleList = Common.PaginatingQueryRecord<CmsSample>;

    type CmsSampleSearchParams = CommonType.RecordNullable<
      Pick<CmsSample, 'subbarcode' | 'orderNo' | 'customerName' | 'productName' | 'sampleType'> &
        Common.CommonSearchParams
    >;

    interface CmsSampleForm {
      id?: number | null;
      /** CMS订单批次主键 */
      cmsBatchId?: string | null;
      /** 正式订单编号 */
      orderNo?: string | null;
      /** 诺禾编号 */
      subbarcode?: string | null;
      /** 订单批次编号 */
      batchNo?: string | null;
      /** 订单业务类型 */
      businessType?: string | null;
      /** 订单收样确认状态 */
      receiveConfirmStatus?: string | null;
      /** 合同甲方客户编码 */
      customerCode?: string | null;
      /** 正式订单客户名称 */
      customerName?: string | null;
      /** 客户类型 */
      customerType?: string | null;
      /** 关联合同编号 */
      contractNo?: string | null;
      /** 关联合同名称 */
      contractName?: string | null;
      /** 研究方案编号 */
      schemeNo?: string | null;
      /** 药企项目编号 */
      projectNo?: string | null;
      /** 研究中心名称 */
      researchCenter?: string | null;
      /** 合同事业部编码 */
      departmentCode?: string | null;
      /** 合同签订日期 */
      contractSignDate?: string | null;
      /** 合同账期 */
      paymentTerm?: string | null;
      /** 合同结算逻辑 */
      checkoutLogic?: string | null;
      /** 合同冻结状态 */
      contractBlockStatus?: string | null;
      /** 合同联系人邮箱 */
      contractContactEmail?: string | null;
      /** 合同运营经理邮箱 */
      operatorEmail?: string | null;
      /** 合同甲方CSS主账号邮箱 */
      customerAccountEmail?: string | null;
      /** 报告接收人 */
      reportReceiver?: string | null;
      /** 报告接收邮箱 */
      reportEmail?: string | null;
      /** 报告接收人电话 */
      reportReceiverPhone?: string | null;
      /** 报告邮寄地址 */
      reportMailingAddress?: string | null;
      /** 运营经理名称 */
      operatorName?: string | null;
      /** 运营经理编码 */
      operatorCode?: string | null;
      /** 合同跟进销售名称 */
      salesName?: string | null;
      /** 合同跟进销售编码 */
      salesCode?: string | null;
      /** 合同关联销售邮箱 */
      salesEmail?: string | null;
      /** 订单保存的销售名称 */
      orderSalesName?: string | null;
      /** 合同技术支持邮箱 */
      supportEmail?: string | null;
      /** 合同经理邮箱 */
      managerEmail?: string | null;
      /** 订单创建人编码 */
      orderRecorderCode?: string | null;
      /** 订单创建人名称 */
      orderRecorderName?: string | null;
      /** 样本记录人名称 */
      sampleRecorderName?: string | null;
      /** 订单销售渠道 */
      salesChannel?: string | null;
      /** 订单销售区域 */
      salesRegion?: string | null;
      /** 送检人 */
      senderName?: string | null;
      /** 送检人联系电话 */
      senderPhone?: string | null;
      /** 送检日期 */
      sendDate?: string | null;
      /** 样本快递公司 */
      expressCompany?: string | null;
      /** 样本快递单号 */
      expressNo?: string | null;
      /** 运输条件 */
      transportCondition?: string | null;
      /** 运输条件编码 */
      transportConditionCode?: string | null;
      /** 是否带温控计 */
      hasTemperatureLogger?: string | null;
      /** 收样实验室 */
      receivingLab?: string | null;
      /** 收样联系人 */
      receivingContact?: string | null;
      /** 收样联系电话 */
      receivingPhone?: string | null;
      /** 收样地址 */
      receivingAddress?: string | null;
      /** 客户采样识别码 */
      customerSamplingCode?: string | null;
      /** 样本名称 */
      sampleName?: string | null;
      /** 试验参与者编号 */
      subjectNo?: string | null;
      /** 性别 */
      sex?: string | null;
      /** 出生年份 */
      birthYear?: string | null;
      /** 临床诊断或疾病类型 */
      diseaseType?: string | null;
      /** 访视周期 */
      visitCycle?: string | null;
      /** 产品编码 */
      productCode?: string | null;
      /** 产品名称 */
      productName?: string | null;
      /** 样本类型 */
      sampleType?: string | null;
      /** 样本类型家族码 */
      sampleTypeCode?: string | null;
      /** 样本数量 */
      sampleQuantity?: string | null;
      /** 数量单位 */
      quantityUnit?: string | null;
      /** 样本属性 */
      sampleAttribute?: string | null;
      /** 取材部位 */
      collectionSite?: string | null;
      /** 组织类型 */
      tissueType?: string | null;
      /** 肿瘤类型 */
      tumorType?: string | null;
      /** 原发部位 */
      primaryTumorSite?: string | null;
      /** 采集日期 */
      collectionDate?: string | null;
      /** 检测方式 */
      assayMethod?: string | null;
      /** 样本备注 */
      remark?: string | null;
      /** 异常备注 */
      abnormalRemark?: string | null;
      /** 管控类型 */
      controlType?: string | null;
      /** 病理编号 */
      pathologyNo?: string | null;
      /** 04类附件路径 */
      pathologyReportPaths?: string | null;
      /** 03类附件路径 */
      otherAttachmentPaths?: string | null;
      /** 订单收样确认时间 */
      receiveConfirmTime?: string | null;
      /** 订单提交时间 */
      orderSubmitTime?: string | null;
      /** 到达日期 */
      arrivalDate?: string | null;
      /** 到达时间 */
      arrivalTime?: string | null;
      /** 实际支付金额 */
      paidAmount?: string | null;
      /** 订单金额 */
      orderAmount?: string | null;
      /** 订单流水标识 */
      paymentSerialNo?: string | null;
      /** 付款完成时间 */
      paymentFinishTime?: string | null;
    }

    interface CmsSampleImportResult {
      total: number;
      inserted: number;
      updated: number;
      failed: number;
      errors: string[];
    }
  }
}
