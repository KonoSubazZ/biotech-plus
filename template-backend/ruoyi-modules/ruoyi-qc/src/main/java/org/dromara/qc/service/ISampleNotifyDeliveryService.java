package org.dromara.qc.service;

import org.dromara.qc.domain.bo.SampleNotifySendBo;
import org.dromara.qc.domain.vo.SampleNotifyMailPreviewVo;
import org.dromara.qc.domain.vo.SampleNotifySendVo;
import org.dromara.qc.domain.vo.SampleNotifySignVo;

/**
 * 样本通知台账 服务层（交付侧：送签 / 刷新签署 / 提交 / 邮件预览 / 发送）
 *
 * @author <你的名字>
 */
public interface ISampleNotifyDeliveryService {

    /**
     * 送签：按通知类型选上上签场景（delay → 报告延迟；其余 → 质控不合格）
     *
     * @param id 通知记录 id
     * @return 送签结果（含合同号）
     */
    SampleNotifySignVo sign(Long id);

    /**
     * 刷新签署状态：已签署则拉取签名后 PDF 落 signed/ 并把 file_path 指过去
     *
     * @param id 通知记录 id
     * @return 刷新结果
     */
    SampleNotifySignVo refreshSign(Long id);

    /**
     * 提交：签名完成后把通知推进到「待处理」，进入异常处理页
     *
     * @param id 通知记录 id
     */
    void submit(Long id);

    /**
     * 发送页参数：发件邮箱 + 收件人/抄送候选 + 默认标题正文 + 附件名
     *
     * @param id 通知记录 id
     * @return 发送页参数
     */
    SampleNotifyMailPreviewVo mailPreview(Long id);

    /**
     * 发送邮件：成功后置「发送成功」，失败置「发送失败」并记录原因（可重发）
     *
     * @param id 通知记录 id
     * @param bo 页面填写的发件邮箱 / 收件人 / 抄送 / 标题 / 正文
     * @return 发送结果
     */
    SampleNotifySendVo send(Long id, SampleNotifySendBo bo);
}
