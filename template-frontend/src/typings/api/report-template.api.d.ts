/**
 * 报告模板配置类型定义（report_template）
 * <p>
 * 一份模板 = 一个 DOCX 实体文件路径（templatePath，预留）+ 一条输出范围配置（moduleCode）。
 * moduleCode 是分号分隔的有序个性化模块编码列表，决定 JSON 里追加哪些个性化字段；留空 = 只输出公共字段。
 * 模板与产品多对多（product_template），由 productIds 维护。
 */
declare namespace Api {
  namespace Report {
    /** 列表项（= 后端 ReportTemplateVo） */
    interface ReportTemplate {
      /** 主键 */
      templateId: number;
      /** 稳定模板编码 */
      templateCode: string;
      /** 模板名称 */
      templateName: string;
      /** 模板版本 */
      templateVersion: string | null;
      /** 客户编码 */
      customerCode: string | null;
      /** 报告类型 */
      reportType: string | null;
      /** 有序个性化模块编码列表（分号分隔） */
      moduleCode: string | null;
      /** 报告命名模板：静态文本 + {{路径}} 动态取值；空 = 用默认命名 */
      reportName: string | null;
      /** DOCX 模板文件路径 */
      templatePath: string;
      /** 模板文件 SHA-256 */
      templateSha256: string | null;
      /** 状态：ENABLED / DISABLED */
      status: string;
      /** 创建时间 */
      createTime: string | null;
      /** 关联产品ID集合 */
      productIds: number[] | null;
      /** 关联产品名称集合（列表展示） */
      productNames: string[] | null;
    }

    /** 搜索参数 */
    type ReportTemplateSearchParams = CommonType.RecordNullable<
      Pick<ReportTemplate, 'templateCode' | 'templateName' | 'reportType' | 'status'> & Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 ReportTemplateBo） */
    interface ReportTemplateForm {
      templateId: number | null;
      templateCode: string;
      templateName: string;
      templateVersion: string;
      customerCode: string;
      reportType: string;
      moduleCode: string;
      reportName: string;
      templatePath: string;
      templateSha256: string;
      status: string;
      /** 关联产品ID集合 */
      productIds: number[];
    }

    /** 报告命名可用变量（后端 ReportNameVariables）：catalog=可用字段变量，sample=样例值，defaultPattern=留空时的默认规则 */
    interface ReportNameVars {
      catalog: string[];
      sample: Record<string, unknown>;
      defaultPattern: string;
    }

    /** 产品下拉选项（product_config） */
    interface ProductOption {
      id: number;
      name: string;
      code: string;
      status: string;
    }
  }
}
