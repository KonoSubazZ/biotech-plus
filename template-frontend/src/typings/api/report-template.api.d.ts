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
      /** 内部模板编码（后端生成 tpl-<主键>，页面不展示、不用前端传） */
      templateCode: string;
      /** 模板名称 */
      templateName: string;
      /** 模板版本 */
      templateVersion: string | null;
      /** 客户编码 */
      customerCode: string | null;
      /** 模板归属项目（project_config.id） */
      projectId: number | null;
      /** 项目编号（列表展示，后端批量回填） */
      projectNo: string | null;
      /** 项目名称（列表展示） */
      projectName: string | null;
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
      /** 状态：active 启用 / inactive 停用 */
      status: string;
      /** 创建时间 */
      createTime: string | null;
      /** 关联产品ID集合 */
      productIds: number[] | null;
      /** 关联产品名称集合（列表展示） */
      productNames: string[] | null;
      /**
       * 关联产品明细（列表「关联产品」列用）：生信编码 productBio 换行产品编码 productCode，
       * 多个产品就一条一条上下排
       */
      products: Api.Report.TemplateProduct[] | null;
    }

    /** 模板关联的一条产品（后端 ReportTemplateVo.products 的一项） */
    interface TemplateProduct {
      productId: number;
      /** 生信编码（= 上传目录第 2 层 product） */
      productBio: string;
      /** 产品名称（对外展示） */
      productName: string;
      /** 产品编码 */
      productCode: string;
    }

    /** 搜索参数 */
    type ReportTemplateSearchParams = CommonType.RecordNullable<
      Pick<ReportTemplate, 'templateName' | 'reportType' | 'status' | 'projectId'> & Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 ReportTemplateBo；templateCode 由后端生成，前端不传） */
    interface ReportTemplateForm {
      templateId: number | null;
      templateName: string;
      templateVersion: string;
      customerCode: string;
      /** 归属项目（必选）：模板先选项目、再选产品 */
      projectId: number | null;
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
      /** 生信编码（= 上传目录第 2 层 product） */
      productBio: string;
      /** 产品名称（对外展示、报告用） */
      productName: string;
      /** 产品编码 */
      productCode: string;
      status: string;
    }

    /** 项目下拉选项（project_config；模板必须先选项目再选产品） */
    interface ProjectOption {
      id: number;
      /** 项目编号（同租户内唯一） */
      projectNo: string;
      /** 项目名称 */
      projectName: string;
      /** 客户名称 */
      customer: string | null;
    }
  }
}
