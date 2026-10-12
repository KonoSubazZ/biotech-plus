/**
 * 报告邮件配置（报告管理 › 邮件配置）类型定义
 * <p>
 * 一个租户可以配多条发件邮箱（列表页增删改），其中一条是「默认邮箱」。
 * 独立一份：report-interpretation.api.d.ts 已经有 500+ 行，两份都声明 Api.Report
 * 命名空间，TypeScript 会自动合并。
 */
declare namespace Api {
  namespace Report {
    /**
     * 邮件配置（= 后端 ReportMailConfigVo）
     * <p>
     * **不含 password**：接口只回 passwordSet（是否已设置），明文永不回显。
     */
    interface ReportMailConfig {
      id: number | null;
      /** 0否 1是 */
      enabled: string | null;
      /** 是否默认发件邮箱：0否 1是（同一租户只有一条为 1） */
      isDefault: string | null;
      smtpHost: string | null;
      smtpPort: number | null;
      /** 0否 1是 */
      sslEnable: string | null;
      fromEmail: string | null;
      fromName: string | null;
      /** 是否已设置密码（只显示「已设置 / 未设置」） */
      passwordSet: boolean | null;
      defaultSubject: string | null;
      defaultContent: string | null;
      /** 创建时间（BaseEntity 带出来） */
      createTime: string | null;
      /** 更新时间 */
      updateTime: string | null;
    }

    /** 邮件配置列表查询参数（与其它模块同写法：实体筛选字段 + 分页通用参数） */
    type ReportMailConfigSearchParams = CommonType.RecordNullable<
      Pick<ReportMailConfig, 'fromEmail'> & Common.CommonSearchParams
    >;

    /** 新增/修改入参（password 传空串 = 不修改已保存的密码） */
    interface ReportMailConfigForm {
      id?: number;
      enabled: string;
      isDefault: string;
      smtpHost: string;
      smtpPort: number;
      sslEnable: string;
      fromEmail: string;
      fromName?: string;
      password?: string;
      defaultSubject?: string;
      defaultContent?: string;
    }

    /** 邮件模板可用变量 + 样例值（提示与试算用） */
    interface MailVariables {
      variables: string[];
      sample: Record<string, string>;
    }

    /**
     * 发件邮箱候选（报告发送页「发件邮箱」下拉项，= 后端 ReportMailSenderOptionVo）
     * <p>
     * subject/content 是这个邮箱自己那套模板按本报告变量渲染好的文案，
     * 页面切换发件邮箱时直接换掉标题/正文，不用再请求。
     */
    interface ReportMailSenderOption {
      configId: number;
      fromEmail: string | null;
      fromName: string | null;
      /** 0否 1是 */
      isDefault: string | null;
      /** 0否 1是；为 0 时下拉里置灰不可选 */
      enabled: string | null;
      subject: string | null;
      content: string | null;
    }
  }
}
