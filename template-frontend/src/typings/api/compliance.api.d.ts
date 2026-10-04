/**
 * 合规管理类型定义
 * <p>
 * 放 src/typings/api/ 下（一个模块一个文件），命名空间 Api.Compliance.<实体>，
 * 与 api.ts / index.vue / search / drawer 共用。不要写成多份或就地声明。
 * 字段与 table dev_log 一一对应（见 script/sql/business/compliance.sql）。
 */
declare namespace Api {
  namespace Compliance {
    /**
     * 开发记录（= 后端 DevLogVo）
     * <p>
     * append-only：不可删除，只能新增/更正（见 ai-rules/04-db-schema.md §2）。
     */
    interface DevLog {
      id: number;
      /** 记录标题 */
      title: string;
      /** 分类：feature 功能新增 / fix 缺陷修复 / change 变更调整 */
      category: string;
      /** 版本号 */
      version?: string | null;
      /** 详细内容 */
      content?: string | null;
      /** 记录文档文件名（原文件名；附件存服务器固定目录，点击可下载） */
      fileName?: string | null;
      /** 开发人员 */
      developer?: string | null;
      /** 记录日期（yyyy-MM-dd） */
      logDate?: string | null;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type DevLogList = Common.PaginatingQueryRecord<DevLog>;

    /** 搜索参数（与项目其它模块保持同一写法） */
    type DevLogSearchParams = CommonType.RecordNullable<
      Pick<DevLog, 'title' | 'category' | 'developer'> & Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 DevLogBo） */
    interface DevLogForm {
      id?: number | null;
      /** 记录标题（必填，表单里校验） */
      title: string;
      /** 分类：feature 功能新增 / fix 缺陷修复 / change 变更调整 */
      category: string;
      /** 版本号 */
      version?: string | null;
      content?: string | null;
      /** 记录文档文件名（由上传接口返回后回填，不在表单里手填） */
      fileName?: string | null;
      developer?: string | null;
      /** 记录日期（yyyy-MM-dd） */
      logDate?: string | null;
    }

    /**
     * 3Q 验证记录（= 后端 ValidationRecordVo）
     * <p>
     * append-only：不可删除，只能新增/更正（见 ai-rules/04-db-schema.md §2）。
     */
    interface ValidationRecord {
      id: number;
      /** 验证类型：IQ 安装确认 / OQ 运行确认 / PQ 性能确认 */
      validationType: string;
      /** 验证标题 */
      title: string;
      /** 版本号（本次验证对应的方案/镜像版本） */
      version?: string | null;
      /** 执行人 */
      executedBy?: string | null;
      /** 执行日期（yyyy-MM-dd） */
      executedDate?: string | null;
      /** 结果：passed 通过 / failed 未通过 / na 不适用 */
      result: string;
      /** 验证摘要 */
      summary?: string | null;
      /** 验证文档文件名（原文件名；附件存服务器固定目录，点击可下载） */
      fileName?: string | null;
      createTime?: string | null;
    }

    /** 分页列表（后端 TableDataInfo 的 rows/total 在顶层） */
    type ValidationRecordList = Common.PaginatingQueryRecord<ValidationRecord>;

    /** 搜索参数（与项目其它模块保持同一写法） */
    type ValidationRecordSearchParams = CommonType.RecordNullable<
      Pick<ValidationRecord, 'validationType' | 'title' | 'executedBy' | 'result'> & Common.CommonSearchParams
    >;

    /** 新增/编辑表单（= 后端 ValidationRecordBo） */
    interface ValidationRecordForm {
      id?: number | null;
      /** 验证类型：IQ 安装确认 / OQ 运行确认 / PQ 性能确认 */
      validationType: string;
      /** 验证标题（必填，表单里校验） */
      title: string;
      version?: string | null;
      executedBy?: string | null;
      /** 执行日期（yyyy-MM-dd） */
      executedDate?: string | null;
      /** 结果：passed 通过 / failed 未通过 / na 不适用 */
      result: string;
      summary?: string | null;
      /** 验证文档文件名（由上传接口返回后回填，不在表单里手填） */
      fileName?: string | null;
    }
  }
}
