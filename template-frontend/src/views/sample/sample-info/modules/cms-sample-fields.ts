/**
 * 样本信息字段元数据：85 个有效字段、6 个分组（cms_sample）。
 * <p>
 * 列表列、列设置、详情/编辑抽屉、空表单都从这里取，避免字段名和中文名在四处漂移。
 * long=true 的字段用 textarea 渲染（长文本/路径）。
 */
export interface CmsSampleField {
  key: keyof Api.Report.CmsSampleForm;
  label: string;
  long?: boolean;
}

export interface CmsSampleGroup {
  key: string;
  name: string;
  fields: CmsSampleField[];
}

export const CMS_SAMPLE_GROUPS: CmsSampleGroup[] = [
  {
    key: 'origin',
    name: '来源身份',
    fields: [
      { key: 'cmsBatchId', label: 'CMS订单批次主键' },
      { key: 'orderNo', label: '正式订单编号' },
      { key: 'subbarcode', label: '诺禾编号' },
      { key: 'batchNo', label: '订单批次编号' },
      { key: 'businessType', label: '订单业务类型' },
      { key: 'receiveConfirmStatus', label: '订单收样确认状态' }
    ]
  },
  {
    key: 'contract',
    name: '客户合同项目',
    fields: [
      { key: 'customerCode', label: '合同甲方客户编码' },
      { key: 'customerName', label: '正式订单客户名称' },
      { key: 'customerType', label: '客户类型' },
      { key: 'contractNo', label: '关联合同编号' },
      { key: 'contractName', label: '关联合同名称' },
      { key: 'schemeNo', label: '研究方案编号' },
      { key: 'projectNo', label: '药企项目编号' },
      { key: 'researchCenter', label: '研究中心名称' },
      { key: 'departmentCode', label: '合同事业部编码' },
      { key: 'contractSignDate', label: '合同签订日期' },
      { key: 'paymentTerm', label: '合同账期' },
      { key: 'checkoutLogic', label: '合同结算逻辑', long: true },
      { key: 'contractBlockStatus', label: '合同冻结状态' }
    ]
  },
  {
    key: 'people',
    name: '人员邮箱',
    fields: [
      { key: 'contractContactEmail', label: '合同联系人邮箱' },
      { key: 'operatorEmail', label: '合同运营经理邮箱' },
      { key: 'customerAccountEmail', label: '合同甲方CSS主账号邮箱' },
      { key: 'reportReceiver', label: '报告接收人' },
      { key: 'reportEmail', label: '报告接收邮箱' },
      { key: 'reportReceiverPhone', label: '报告接收人电话' },
      { key: 'reportMailingAddress', label: '报告邮寄地址', long: true },
      { key: 'operatorName', label: '运营经理名称' },
      { key: 'operatorCode', label: '运营经理编码' },
      { key: 'salesName', label: '合同跟进销售名称' },
      { key: 'salesCode', label: '合同跟进销售编码' },
      { key: 'salesEmail', label: '合同关联销售邮箱' },
      { key: 'orderSalesName', label: '订单保存的销售名称' },
      { key: 'supportEmail', label: '合同技术支持邮箱' },
      { key: 'managerEmail', label: '合同经理邮箱' },
      { key: 'orderRecorderCode', label: '订单创建人编码' },
      { key: 'orderRecorderName', label: '订单创建人名称' },
      { key: 'sampleRecorderName', label: '样本记录人名称' },
      { key: 'salesChannel', label: '订单销售渠道' },
      { key: 'salesRegion', label: '订单销售区域' }
    ]
  },
  {
    key: 'shipping',
    name: '送检运输',
    fields: [
      { key: 'senderName', label: '送检人' },
      { key: 'senderPhone', label: '送检人联系电话' },
      { key: 'sendDate', label: '送检日期' },
      { key: 'expressCompany', label: '样本快递公司' },
      { key: 'expressNo', label: '样本快递单号' },
      { key: 'transportCondition', label: '运输条件' },
      { key: 'transportConditionCode', label: '运输条件编码' },
      { key: 'hasTemperatureLogger', label: '是否带温控计' },
      { key: 'receivingLab', label: '收样实验室' },
      { key: 'receivingContact', label: '收样联系人' },
      { key: 'receivingPhone', label: '收样联系电话' },
      { key: 'receivingAddress', label: '收样地址', long: true }
    ]
  },
  {
    key: 'sample',
    name: '样本受试者',
    fields: [
      { key: 'customerSamplingCode', label: '客户采样识别码' },
      { key: 'sampleName', label: '样本名称' },
      { key: 'subjectNo', label: '试验参与者编号' },
      { key: 'sex', label: '性别' },
      { key: 'birthYear', label: '出生年份' },
      { key: 'diseaseType', label: '临床诊断或疾病类型' },
      { key: 'visitCycle', label: '访视周期' },
      { key: 'productCode', label: '产品编码' },
      { key: 'productName', label: '产品名称' },
      { key: 'sampleType', label: '样本类型' },
      { key: 'sampleTypeCode', label: '样本类型家族码' },
      { key: 'sampleQuantity', label: '样本数量' },
      { key: 'quantityUnit', label: '数量单位' },
      { key: 'sampleAttribute', label: '样本属性' },
      { key: 'collectionSite', label: '取材部位' },
      { key: 'tissueType', label: '组织类型' },
      { key: 'tumorType', label: '肿瘤类型' },
      { key: 'primaryTumorSite', label: '原发部位' },
      { key: 'collectionDate', label: '采集日期' },
      { key: 'assayMethod', label: '检测方式', long: true },
      { key: 'remark', label: '样本备注', long: true },
      { key: 'abnormalRemark', label: '异常备注', long: true },
      { key: 'controlType', label: '管控类型' },
      { key: 'pathologyNo', label: '病理编号' },
      { key: 'pathologyReportPaths', label: '04类附件路径', long: true },
      { key: 'otherAttachmentPaths', label: '03类附件路径', long: true }
    ]
  },
  {
    key: 'finance',
    name: '时间财务',
    fields: [
      { key: 'receiveConfirmTime', label: '订单收样确认时间' },
      { key: 'orderSubmitTime', label: '订单提交时间' },
      { key: 'arrivalDate', label: '到达日期' },
      { key: 'arrivalTime', label: '到达时间' },
      { key: 'paidAmount', label: '实际支付金额' },
      { key: 'orderAmount', label: '订单金额' },
      { key: 'paymentSerialNo', label: '订单流水标识' },
      { key: 'paymentFinishTime', label: '付款完成时间' }
    ]
  }
];

export const CMS_SAMPLE_FIELDS = CMS_SAMPLE_GROUPS.flatMap(group => group.fields);

/** 列表默认展示的 15 个关键列（顺序即页面展示顺序） */
export const DEFAULT_VISIBLE_COLUMNS: string[] = [
  'subbarcode',
  'sampleName',
  'productName',
  'birthYear',
  'sex',
  'sampleType',
  'diseaseType',
  'sampleAttribute',
  'collectionSite',
  'primaryTumorSite',
  'customerName',
  'projectNo',
  'researchCenter',
  'collectionDate',
  'remark'
];

/** 空表单：只有业务键给空串，其余 null */
export function createEmptyCmsSampleForm(): Api.Report.CmsSampleForm {
  const form: Api.Report.CmsSampleForm = {};
  CMS_SAMPLE_FIELDS.forEach(field => {
    form[field.key] = null;
  });
  form.subbarcode = '';
  return form;
}
