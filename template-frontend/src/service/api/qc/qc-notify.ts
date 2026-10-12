import { request } from '@/service/request';

/**
 * 样本质控不合格通知（处理）接口层
 * <p>
 * 页面只做三件事：打开弹窗（预填）→ 编辑 → 生成文档；生成会落一条记录（流水），
 * 已生成过的可以按记录 id 下载：优先 PDF（后端已转出），没有 PDF 时退回 DOCX。
 */

/**
 * 处理弹窗预填：按样本编号回填 sample_file 字段 + 默认值 + 上次生成内容
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
