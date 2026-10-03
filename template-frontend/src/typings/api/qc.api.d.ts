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
      /** 质控项目名称（与 qc_record.qc_item 对应） */
      qcItem: string;
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
      qcCategory: string;
      minValue?: string | null;
      maxValue?: string | null;
      unit?: string | null;
      status?: string | null;
      remark?: string | null;
    }

    /**
     * 质控记录（= 后端 QcRecordVo）
     * <p>
     * 湿实验质控与生信质控共用同一张表/同一套接口，靠 qcCategory 区分
     * （见 docs/context/设计文档/bioinfo-qc.md）。字段对应 script/sql/business/qc-record.sql。
     */
    interface QcRecord {
      id: number;
      /** 样本条码 */
      subbarcode: string;
      /** 质控项目名称（如 mapping_rate、average_depth） */
      qcItem: string;
      /** 质控结果数值（字符串形式，如 "98.5"） */
      qcResult?: string | null;
      /** 操作员 */
      operator?: string | null;
      /** 检测时间 */
      testedAt?: string | null;
      /** 备注 */
      remark?: string | null;
      /** 人工状态：pending 待确认 / passed 通过 / failed 未通过 */
      status: string;
      /** 质控类别：wet_lab 湿实验 / bioinfo 生信 */
      qcCategory: string;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type QcRecordList = Common.PaginatingQueryRecord<QcRecord>;

    /** 搜索参数 */
    type QcRecordSearchParams = CommonType.RecordNullable<
      Pick<QcRecord, 'subbarcode' | 'qcItem' | 'status' | 'qcCategory'> & Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 QcRecordBo） */
    interface QcRecordForm {
      id?: number | null;
      subbarcode: string;
      qcItem: string;
      qcResult?: string | null;
      operator?: string | null;
      testedAt?: string | null;
      remark?: string | null;
      /** 人工状态：pending 待确认 / passed 通过 / failed 未通过 */
      status?: string | null;
      /** 质控类别：由所在页面固定传入，不在表单里编辑 */
      qcCategory?: string | null;
    }
  }
}
