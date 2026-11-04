package org.dromara.report.service;

import org.dromara.report.domain.DeliveryEmailLog;
import org.dromara.report.domain.vo.DeliveryEmailLogVo;

import java.util.List;

/**
 * 交付物邮件发送记录（报告 + 通知共用）
 *
 * @author <你的名字>
 */
public interface IDeliveryEmailLogService {

    /** 交付物类型：报告 */
    String TARGET_REPORT = "report";

    /** 交付物类型：通知 */
    String TARGET_NOTIFY = "notify";

    /**
     * 发信前落一条 SENDING（发信失败的痕迹才有落点）。
     *
     * @param row 记录（status / requestedAt 由这里补默认值）
     * @return 记录 id
     */
    Long start(DeliveryEmailLog row);

    /** 发信成功：SUCCESS + messageId + finishedAt */
    void markSuccess(Long id, String messageId);

    /** 发信失败：FAILED + 原因 + finishedAt */
    void markFailed(Long id, String reason);

    /** 按交付物查发送记录（时间倒序） */
    List<DeliveryEmailLogVo> listByTarget(String targetType, Long targetId);
}
