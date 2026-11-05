package org.dromara.report.service.impl.revision;

import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.report.domain.vo.ReportTemplateVo;
import org.dromara.report.mapper.ReportRevisionMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ReportVersionAllocator {
    private final ReportRevisionMapper mapper;

    /** 事务只覆盖占号及清除旧有效引用，不跨 Python/文件转换。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> begin(Map<String, Object> report, ReportTemplateVo template) {
        if (report.get("reportVersion") != null
            && !Objects.equals(number(report.get("templateId")), template.getTemplateId())) {
            throw new ServiceException("已占用业务版本的报告不能切换模板");
        }
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("tenantId", report.get("tenantId"));
        key.put("client", template.getCustomerCode());
        key.put("subbarcode", report.get("subbarcode"));
        key.put("templateId", template.getTemplateId());
        key.put("reportId", report.get("reportId"));
        if (key.get("client") == null || key.get("client").toString().isBlank()) {
            throw new ServiceException("模板客户编码不能为空，无法分配报告版本");
        }
        mapper.ensureSequence(key);
        Map<String, Object> sequence = mapper.lockSequence(key);
        Long owner = number(sequence.get("activeReportId"));
        Long reportId = number(report.get("reportId"));
        if (owner != null && !owner.equals(reportId)) {
            throw new ServiceException("同客户、样本及模板已有在制报告：" + owner);
        }
        Long sequenceId = number(sequence.get("id"));
        if (report.get("reportSequenceId") != null
            && !sequenceId.equals(number(report.get("reportSequenceId")))) {
            throw new ServiceException("报告客户或版本序列已变化，请恢复原模板客户配置");
        }
        key.put("sequenceId", sequenceId);
        key.put("reportVersion", number(sequence.get("sentVersion")) + 1);
        if (mapper.claimSequence(sequenceId, reportId) != 1 || mapper.startGeneration(key) != 1) {
            throw new ServiceException("报告状态已变化，请刷新后重试");
        }
        return key;
    }

    @Transactional(rollbackFor = Exception.class)
    public void sent(Long reportId, Long userId, boolean versioned) {
        if (versioned && mapper.completeVersion(reportId) != 1) {
            throw new ServiceException("报告版本登记冲突，邮件可能已发出，请核对发送日志，勿直接重发");
        }
        if (mapper.markSent(reportId, userId) != 1) {
            throw new ServiceException("报告发送状态登记冲突，请核对发送日志");
        }
    }

    private Long number(Object value) {
        if (value == null) {
            return null; // 可选的序列归属/模板ID尚未分配。
        }
        return Long.valueOf(value.toString());
    }

    /** 名称主体按序列固化，模板配置变化不改变已占号报告或其后续版本的文件名。 */
    @Transactional(rollbackFor = Exception.class)
    public String name(Map<String, Object> report, String proposed) {
        Long sequenceId = number(report.get("reportSequenceId"));
        Map<String, Object> sequence = mapper.lockSequenceById(sequenceId);
        if (sequence == null || !Objects.equals(number(report.get("reportId")),
            number(sequence.get("activeReportId")))) {
            throw new ServiceException("报告版本不属于当前占号记录");
        }
        String baseName = (String) sequence.get("baseName");
        int version = Integer.parseInt(report.get("reportVersion").toString());
        if (baseName == null) {
            String safeBase = ReportRevisionPolicy.baseName(proposed);
            String fileName = ReportRevisionPolicy.fileName(safeBase, version);
            if (mapper.rememberBaseName(sequenceId, safeBase) != 1) {
                throw new ServiceException("报告名称登记冲突，请刷新重试");
            }
            return fileName;
        }
        return ReportRevisionPolicy.fileName(baseName, version);
    }

}
