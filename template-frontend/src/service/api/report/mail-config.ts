import { request } from '@/service/request';

/**
 * 报告邮件配置接口层（报告管理 › 邮件配置）
 * <p>
 * 约定同其它模块：函数名 fetch + 动词 + 实体；返回类型写在泛型里。
 * 一个租户可以配多条发件邮箱；列表与详情都不回显密码（只给 passwordSet）。
 */

/** 分页查询邮件配置（默认邮箱排最前） */
export function fetchGetReportMailConfigList(params: Api.Report.ReportMailConfigSearchParams) {
  return request<Api.Common.PaginatingQueryRecord<Api.Report.ReportMailConfig>>({
    url: '/report/mail-config/list',
    method: 'get',
    params
  });
}

/** 按 id 查一条（编辑回显用） */
export function fetchGetReportMailConfig(id: number) {
  return request<Api.Report.ReportMailConfig>({
    url: `/report/mail-config/${id}`,
    method: 'get'
  });
}

/** 新增一条发件邮箱 */
export function fetchAddReportMailConfig(data: Api.Report.ReportMailConfigForm) {
  return request<null>({
    url: '/report/mail-config',
    method: 'post',
    data
  });
}

/** 修改一条发件邮箱（password 传空串 = 不修改已保存的密码） */
export function fetchUpdateReportMailConfig(data: Api.Report.ReportMailConfigForm) {
  return request<null>({
    url: '/report/mail-config',
    method: 'put',
    data
  });
}

/** 删除一条发件邮箱（删掉的若是默认，后端会自动把最早的一条补成默认） */
export function fetchDeleteReportMailConfig(id: number) {
  return request<null>({
    url: `/report/mail-config/${id}`,
    method: 'delete'
  });
}

/** 设为默认邮箱（同租户其它行会被清成非默认） */
export function fetchSetDefaultReportMailConfig(id: number) {
  return request<null>({
    url: `/report/mail-config/${id}/default`,
    method: 'put'
  });
}

/** 邮件模板可用变量 + 样例值（页面提示与试算用；同一份也是后端保存期校验口径） */
export function fetchGetReportMailVariables() {
  return request<Api.Report.MailVariables>({
    url: '/report/mail-config/variables',
    method: 'get'
  });
}

/** 发送测试邮件（configId 不传 = 用默认邮箱） */
export function fetchSendTestMail(data: { to: string; configId?: number }) {
  return request<null>({
    url: '/report/mail-config/test',
    method: 'post',
    data
  });
}
