package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.satoken.utils.LoginHelper;
import org.dromara.report.domain.DeliveryEmailLog;
import org.dromara.report.domain.vo.DeliveryEmailLogVo;
import org.dromara.report.mapper.DeliveryEmailLogMapper;
import org.dromara.report.service.IDeliveryEmailLogService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 交付物邮件发送记录实现
 * <p>
 * 只做「发信前 SENDING / 发信后 SUCCESS|FAILED」三段式，不做重试；
 * 重发由业务侧再调一次 start，因此同一交付物可以有多行。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryEmailLogServiceImpl implements IDeliveryEmailLogService {

    private final DeliveryEmailLogMapper baseMapper;

    @Override
    public Long start(DeliveryEmailLog row) {
        if (row.getTraceId() == null || row.getTraceId().isBlank()) {
            row.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
        row.setStatus("SENDING");
        row.setRequestedAt(row.getRequestedAt() == null ? new Date() : row.getRequestedAt());
        row.setTriggeredBy(row.getTriggeredBy() == null ? LoginHelper.getUsername() : row.getTriggeredBy());
        row.setRecipientCount(row.getRecipientCount() == null ? 0L : row.getRecipientCount());
        baseMapper.insert(row);
        return row.getId();
    }

    @Override
    public void markSuccess(Long id, String messageId) {
        baseMapper.update(null, Wrappers.<DeliveryEmailLog>lambdaUpdate()
            .eq(DeliveryEmailLog::getId, id)
            .set(DeliveryEmailLog::getStatus, "SUCCESS")
            .set(DeliveryEmailLog::getProviderMessageId, messageId)
            .set(DeliveryEmailLog::getFinishedAt, new Date()));
    }

    @Override
    public void markFailed(Long id, String reason) {
        if (id == null) {
            return;
        }
        baseMapper.update(null, Wrappers.<DeliveryEmailLog>lambdaUpdate()
            .eq(DeliveryEmailLog::getId, id)
            .set(DeliveryEmailLog::getStatus, "FAILED")
            .set(DeliveryEmailLog::getFailureReason, truncate(reason))
            .set(DeliveryEmailLog::getFinishedAt, new Date()));
    }

    @Override
    public List<DeliveryEmailLogVo> listByTarget(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            throw new ServiceException("查询发送记录必须指定交付物类型与ID", 400);
        }
        return baseMapper.selectVoList(Wrappers.<DeliveryEmailLog>lambdaQuery()
            .eq(DeliveryEmailLog::getTargetType, targetType)
            .eq(DeliveryEmailLog::getTargetId, targetId)
            .orderByDesc(DeliveryEmailLog::getRequestedAt)
            .orderByDesc(DeliveryEmailLog::getId));
    }

    private String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() > 1000 ? reason.substring(0, 1000) : reason;
    }
}
