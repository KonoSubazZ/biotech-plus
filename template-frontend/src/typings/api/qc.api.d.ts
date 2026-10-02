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
  }
}
