import { request } from '@/service/request';

/**
 * 样本通知台账接口层（四类通知：延期 / 样本不合格 / 湿实验不合格 / 干实验不合格）
 * <p>
 * 生成侧：弹窗预填 → 生成（JSON→DOCX→PDF）→ 送签 → 刷新签署 → 提交（待处理）；
 * 交付侧：异常处理页 → 邮件预览 → 发送。
 */

/**
 * 处理弹窗预填：按样本编号回填 cms_sample 字段 + 默认值 + 上次生成内容
 *
 * @param subbarcode 样本编号
 * @param notifyType 通知类型（delay / sample / wet_lab / bioinfo）
 */
export function fetchGetQcNotifyPrefill(subbarcode: string, notifyType: string) {
  return request<Api.Qc.NotifyPrefill>({
    url: '/qc/notify/prefill',
    method: 'get',
    params: { subbarcode, notifyType }
  });
}

/** 保存并生成通知文档（JSON → docxtpl → DOCX → PDF；同日未签名未提交可覆盖） */
export function fetchGenerateQcNotify(data: Api.Qc.NotifyForm) {
  return request<Api.Qc.NotifyResult>({
    url: '/qc/notify/generate',
    method: 'post',
    data
  });
}

/**
 * 送签（上上签电子签署）
 * <p>
 * 前置：该记录已生成通知且有 PDF；delay 走「报告延迟」场景，其余走「质控不合格」场景。
 *
 * @param id 通知记录 id（sample_notify.id）
 */
export function fetchSignQcNotify(id: string) {
  return request<Api.Qc.NotifySignResult>({
    url: `/qc/notify/${id}/sign`,
    method: 'post'
  });
}

/**
 * 刷新签署状态：已签署则后端拉取签名件落到 notify/signed/ 并置「已签署」
 *
 * @param id 通知记录 id（sample_notify.id）
 */
export function fetchRefreshQcNotifySign(id: string) {
  return request<Api.Qc.NotifySignResult>({
    url: `/qc/notify/${id}/refresh-sign`,
    method: 'post'
  });
}

/**
 * 提交：签名完成后把通知推进到「待处理」，进入异常处理页
 *
 * @param id 通知记录 id
 */
export function fetchSubmitQcNotify(id: number | string) {
  return request<void>({
    url: `/qc/notify/${id}/submit`,
    method: 'post'
  });
}

/** 异常处理分页（只返回已签名且已提交的记录） */
export function fetchGetNotifyPage(params: Api.Qc.NotifyQuery) {
  return request<Api.Common.PaginatingQueryRecord<Api.Qc.NotifyRow>>({
    url: '/qc/notify/page',
    method: 'get',
    params
  });
}

/** 通知详情 */
export function fetchGetNotifyDetail(id: number | string) {
  return request<Api.Qc.NotifyRow>({
    url: `/qc/notify/${id}`,
    method: 'get'
  });
}

/** 发送页参数（发件邮箱 / 收件人 / 抄送 / 标题 / 正文 / 附件名） */
export function fetchGetNotifyMailPreview(id: number | string) {
  return request<Api.Qc.NotifyMailPreview>({
    url: `/qc/notify/${id}/mail-preview`,
    method: 'get'
  });
}

/** 发送邮件（成功→发送成功；失败→发送失败，可重发） */
export function fetchSendNotify(id: number | string, data: Api.Qc.NotifySendForm) {
  return request<Api.Qc.NotifySendResult>({
    url: `/qc/notify/${id}/send`,
    method: 'post',
    data
  });
}
