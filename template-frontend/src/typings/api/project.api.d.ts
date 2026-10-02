/**
 * 产品配置类型定义
 * <p>
 * 放 src/typings/ 下，命名空间 Api.<模块>.<实体>，与 api.ts、index.vue 共用。
 * 不要写成多份、也不要在组件里就地声明。
 */
declare namespace Api {
  namespace Project {
    /** 列表项（= 后端 Vo） */
    interface ProductConfig {
      id: number;
      name: string;
      code: string;
      /** active 启用 / inactive 停用 */
      status: string;
      testType?: string | null;
      relatedDiseases?: string | null;
      reportCycleDays?: number | null;
      remark?: string | null;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type ProductConfigList = Common.PaginatingQueryRecord<ProductConfig>;

    /** 搜索参数（与项目其它模块同一写法：分页走 Common.CommonSearchParams） */
    type ProductConfigSearchParams = CommonType.RecordNullable<
      Pick<ProductConfig, 'name' | 'code' | 'testType' | 'status'> & Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 Bo） */
    interface ProductConfigForm {
      id?: number | null;
      name: string;
      code: string;
      testType?: string | null;
      relatedDiseases?: string | null;
      reportCycleDays?: number | null;
      status?: string | null;
      remark?: string | null;
    }
  }
}
