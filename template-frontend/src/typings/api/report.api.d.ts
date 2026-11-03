/**
 * 样本信息类型定义（cms_sample，药企 CMS 接口字段 + 公共字段）
 * <p>
 * 只包含 85 个 biotech_* 有效字段；59 个 deprecated_* 兼容列不在前端类型里。
 */
declare namespace Api {
  namespace Report {
    interface CmsSample {
      id: number;
      /** 订单批次编号 */
      batchNo: string | null;
      /** 客户名称 */
      customerName: string | null;
      /** 合同名称 */
      contractName: string | null;
      /** 合同编号 */
      contractNo: string | null;
      /** 研究方案编号 */
      schemeNo: string | null;
      /** 项目编号 */
      projectNo: string | null;
      /** 联系人邮箱 */
      contractContactEmail: string | null;
      /** 运营经理 */
      operatorName: string | null;
      /** 研究中心名称 */
      researchCenter: string | null;
      /** 报告接收人 */
      reportReceiver: string | null;
      /** 报告接收邮箱 */
      reportEmail: string | null;
      /** 送检人 */
      senderName: string | null;
      /** 联系电话 */
      senderPhone: string | null;
      /** 送检日期 */
      sendDate: string | null;
      /** 快递公司 */
      expressCompany: string | null;
      /** 快递单号 */
      expressNo: string | null;
      /** 快递条件 */
      transportCondition: string | null;
      /** 是否带温控计 */
      hasTemperatureLogger: string | null;
      /** 收样确认时间 */
      receiveConfirmTime: string | null;
      /** 提交时间 */
      orderSubmitTime: string | null;
      /** 订单编号 */
      orderNo: string | null;
      /** 诺禾编号 */
      subbarcode: string | null;
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
      /** 临床诊断 */
      diseaseType: string | null;
      /** 访视周期 */
      visitCycle: string | null;
      /** 产品编码 */
      productCode: string | null;
      /** 产品名称 */
      productName: string | null;
      /** 样本类型 */
      sampleType: string | null;
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
      /** 备注 */
      remark: string | null;
      /** 异常备注 */
      abnormalRemark: string | null;
      /** 管控类型 */
      controlType: string | null;
      /** 到达时间 */
      arrivalTime: string | null;
      /** 最近一次同步/刷新的变更明细（JSON 数组字符串） */
      syncLog?: string | null;
    }

    /** 单个字段的变更（sync_log 数组元素） */
    interface CmsFieldChange {
      field: string;
      label: string;
      oldValue: string | null;
      newValue: string | null;
      by: string;
      at: string;
    }

    /** 行内「刷新」的返回 */
    interface CmsSampleRefresh {
      changed: boolean;
      changes: CmsFieldChange[];
      by: string;
      at: string;
      sample: CmsSample | null;
      /** 同订单其他样本（双样本联动刷新）的结果，单样本订单为空 */
      siblings?: CmsSampleRefresh[] | null;
    }

    type CmsSampleList = Common.PaginatingQueryRecord<CmsSample>;

    type CmsSampleSearchParams = CommonType.RecordNullable<
      Pick<CmsSample, 'subbarcode' | 'orderNo' | 'customerName' | 'productName' | 'sampleType'> &
        Common.CommonSearchParams
    >;

    interface CmsSampleForm {
      id?: number;
      /** 订单批次编号 */
      batchNo?: string | null;
      /** 客户名称 */
      customerName?: string | null;
      /** 合同名称 */
      contractName?: string | null;
      /** 合同编号 */
      contractNo?: string | null;
      /** 研究方案编号 */
      schemeNo?: string | null;
      /** 项目编号 */
      projectNo?: string | null;
      /** 联系人邮箱 */
      contractContactEmail?: string | null;
      /** 运营经理 */
      operatorName?: string | null;
      /** 研究中心名称 */
      researchCenter?: string | null;
      /** 报告接收人 */
      reportReceiver?: string | null;
      /** 报告接收邮箱 */
      reportEmail?: string | null;
      /** 送检人 */
      senderName?: string | null;
      /** 联系电话 */
      senderPhone?: string | null;
      /** 送检日期 */
      sendDate?: string | null;
      /** 快递公司 */
      expressCompany?: string | null;
      /** 快递单号 */
      expressNo?: string | null;
      /** 快递条件 */
      transportCondition?: string | null;
      /** 是否带温控计 */
      hasTemperatureLogger?: string | null;
      /** 收样确认时间 */
      receiveConfirmTime?: string | null;
      /** 提交时间 */
      orderSubmitTime?: string | null;
      /** 订单编号 */
      orderNo?: string | null;
      /** 诺禾编号 */
      subbarcode?: string | null;
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
      /** 临床诊断 */
      diseaseType?: string | null;
      /** 访视周期 */
      visitCycle?: string | null;
      /** 产品编码 */
      productCode?: string | null;
      /** 产品名称 */
      productName?: string | null;
      /** 样本类型 */
      sampleType?: string | null;
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
      /** 备注 */
      remark?: string | null;
      /** 异常备注 */
      abnormalRemark?: string | null;
      /** 管控类型 */
      controlType?: string | null;
      /** 到达时间 */
      arrivalTime?: string | null;
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
