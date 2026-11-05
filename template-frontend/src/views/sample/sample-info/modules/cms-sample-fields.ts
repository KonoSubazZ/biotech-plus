/**
 * 样本信息字段元数据：45 个设计字段、5 个分组（cms_sample）。
 * <p>
 * 列表列、列设置、详情/编辑抽屉、空表单都从这里取，避免字段名和中文名在四处漂移。
 * long=true 的字段用 textarea 渲染（长文本）。
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
    key: 'order',
    name: '订单合同',
    fields: [
      { key: 'orderNo', label: '订单编号' },
      { key: 'batchNo', label: '订单批次编号' },
      { key: 'customerName', label: '客户名称' },
      { key: 'contractName', label: '合同名称' },
      { key: 'contractNo', label: '合同编号' },
      { key: 'schemeNo', label: '研究方案编号' },
      { key: 'projectNo', label: '项目编号' },
      { key: 'researchCenter', label: '研究中心名称' },
      { key: 'operatorName', label: '运营经理' },
      { key: 'contractContactEmail', label: '联系人邮箱' }
    ]
  },
  {
    key: 'shipping',
    name: '送检运输',
    fields: [
      { key: 'senderName', label: '送检人' },
      { key: 'senderPhone', label: '联系电话' },
      { key: 'sendDate', label: '送检日期' },
      { key: 'expressCompany', label: '快递公司' },
      { key: 'expressNo', label: '快递单号' },
      { key: 'transportCondition', label: '快递条件' },
      { key: 'hasTemperatureLogger', label: '是否带温控计' }
    ]
  },
  {
    key: 'sample',
    name: '样本受试者',
    fields: [
      { key: 'subbarcode', label: '诺禾编号' },
      { key: 'customerSamplingCode', label: '客户采样识别码' },
      { key: 'sampleName', label: '样本名称' },
      { key: 'subjectNo', label: '试验参与者编号' },
      { key: 'sex', label: '性别' },
      { key: 'birthYear', label: '出生年份' },
      { key: 'diseaseType', label: '临床诊断' },
      { key: 'visitCycle', label: '访视周期' },
      { key: 'sampleType', label: '样本类型' },
      { key: 'sampleQuantity', label: '样本数量' },
      { key: 'quantityUnit', label: '数量单位' },
      { key: 'sampleAttribute', label: '样本属性' },
      { key: 'collectionSite', label: '取材部位' },
      { key: 'tissueType', label: '组织类型' },
      { key: 'tumorType', label: '肿瘤类型' },
      { key: 'primaryTumorSite', label: '原发部位' },
      { key: 'collectionDate', label: '采集日期' },
      { key: 'assayMethod', label: '检测方式' }
    ]
  },
  {
    key: 'product',
    name: '产品与备注',
    fields: [
      { key: 'productCode', label: '产品编码' },
      { key: 'productName', label: '产品名称' },
      { key: 'controlType', label: '管控类型' },
      { key: 'remark', label: '备注', long: true },
      { key: 'abnormalRemark', label: '异常备注', long: true }
    ]
  },
  {
    key: 'report',
    name: '报告与时间',
    fields: [
      { key: 'reportReceiver', label: '报告接收人' },
      { key: 'reportEmail', label: '报告接收邮箱' },
      { key: 'receiveConfirmTime', label: '收样确认时间' },
      { key: 'orderSubmitTime', label: '提交时间' },
      { key: 'arrivalTime', label: '到达时间' }
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
  const form = {} as Record<string, unknown>;
  CMS_SAMPLE_FIELDS.forEach(field => {
    form[field.key] = null;
  });
  form.subbarcode = '';
  return form as Api.Report.CmsSampleForm;
}
