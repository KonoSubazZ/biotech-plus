import { request } from '@/service/request';

/**
 * 样本质控不合格通知（处理）接口层
 * <p>
 * 页面只做三件事：打开弹窗（预填）→ 编辑 → 生成文档；生成会落一条记录（流水），
 * 已生成过的可以按记录 id 下载：优先 PDF（后端已转出），没有 PDF 时退回 DOCX。
 */

/**
 * 处理弹窗预填：按样本编号回填 cms_sample 字段 + 默认值 + 上次生成内容
 *
 * @param subbarcode 样本编号
 * @param qcCategory 质控类型（wet_lab 湿实验 / bioinfo 生信）：决定取哪张质控表的记录与不达标原因
 */
export function fetchGetQcNotifyPrefill(subbarcode: string, qcCategory: string) {
  return request<Api.Qc.NotifyPrefill>({
    url: '/qc/notify/prefill',
    method: 'get',
    params: { subbarcode, qcCategory }
  });
}

/** 保存并生成通知文档（JSON → docxtpl → DOCX） */
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
 * 前置：该记录已生成通知且有 PDF（后端校验，PDF 缺失会报错提示重新生成）；
 * 成功返回送签拿到的合同号，重复送签后端会报错（换签请重新生成通知）。
 *
 * @param id 通知记录 id（qc_failed_notify.id）
 */
export function fetchSignQcNotify(id: string) {
  return request<Api.Qc.NotifySignResult>({
    url: `/qc/notify/${id}/sign`,
    method: 'post'
  });
}

/**
 * 刷新签署状态：已签署则后端拉取签名后 PDF 落到 notify/signed/ 并置「已签署」
 *
 * @param id 通知记录 id（qc_failed_notify.id）
 */
export function fetchRefreshQcNotifySign(id: string) {
  return request<Api.Qc.NotifySignResult>({
    url: `/qc/notify/${id}/refresh-sign`,
    method: 'post'
  });
}
