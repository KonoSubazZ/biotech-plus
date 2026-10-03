/**
 * 样本信息类型定义
 * <p>
 * 放 src/typings/api/ 下（一个模块一个文件），命名空间 Api.<模块>.<实体>，
 * 与 api.ts / index.vue / search / drawer / import-modal 共用。
 * 字段与 table sample_file 一一对应（见 script/sql/business/report.sql、
 * 设计文档 docs/context/设计文档/spec.md §1.4）。
 */
declare namespace Api {
  namespace Report {
    /** 列表项（= 后端 SampleInfoVo） */
    interface SampleInfo {
      id: number;
      /** 样本编号（subbarcode；质控记录 qc_record.subbarcode 关联它） */
      subbarcode: string;
      /** 条码 */
      barcode?: string | null;
      /** 患者编号 */
      patientId?: string | null;
      /** 患者姓名 */
      personName?: string | null;
      /** 性别 */
      gender?: string | null;
      /** 出生日期（yyyy-MM-dd） */
      birthday?: string | null;
      /** 年龄 */
      age?: string | null;
      /** 患者电话 */
      patientPhone?: string | null;
      /** 医院 */
      hospital?: string | null;
      /** 接收日期（yyyy-MM-dd） */
      receivedDate?: string | null;
      /** 样本类型 */
      specimenType?: string | null;
      /** 样本数量 */
      specimenQuantity?: string | null;
      /** 检测方案 */
      testingProgram?: string | null;
      /** 疾病类型（录单癌种） */
      diseaseType?: string | null;
      /** 客户 */
      client?: string | null;
      /** 委托日期（yyyy-MM-dd；搜索栏的「日期」按它筛选） */
      commissionDate?: string | null;
      /** 产品名称（录单产品） */
      productName?: string | null;
      /** 备注 */
      remark?: string | null;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type SampleInfoList = Common.PaginatingQueryRecord<SampleInfo>;

    /**
     * 搜索参数
     * <p>
     * 委托日期范围走 params.beginTime / params.endTime（与系统其它页面的日期范围同一写法），
     * 后端 SampleInfoServiceImpl.buildQueryWrapper 从 bo.params 里读。
     */
    type SampleInfoSearchParams = CommonType.RecordNullable<
      Pick<SampleInfo, 'subbarcode' | 'personName' | 'client' | 'diseaseType' | 'productName'> &
        Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 SampleInfoBo） */
    interface SampleInfoForm {
      id?: number | null;
      subbarcode: string;
      barcode?: string | null;
      patientId?: string | null;
      personName?: string | null;
      gender?: string | null;
      birthday?: string | null;
      age?: string | null;
      patientPhone?: string | null;
      hospital?: string | null;
      receivedDate?: string | null;
      specimenType?: string | null;
      specimenQuantity?: string | null;
      testingProgram?: string | null;
      diseaseType?: string | null;
      client?: string | null;
      commissionDate?: string | null;
      productName?: string | null;
      remark?: string | null;
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
