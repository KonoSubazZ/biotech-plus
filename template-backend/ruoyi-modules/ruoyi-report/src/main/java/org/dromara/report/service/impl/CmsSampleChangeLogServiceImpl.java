package org.dromara.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.report.domain.CmsSampleChangeLog;
import org.dromara.report.domain.vo.CmsFieldChangeVo;
import org.dromara.report.domain.vo.CmsSampleChangeLogVo;
import org.dromara.report.mapper.CmsSampleChangeLogMapper;
import org.dromara.report.service.ICmsSampleChangeLogService;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 样本信息变更流水实现
 * <p>
 * 只做追加与查询，不做去重/覆盖：同一字段多次变化就是多行，历史才完整。
 *
 * @author <你的名字>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmsSampleChangeLogServiceImpl implements ICmsSampleChangeLogService {

    private final CmsSampleChangeLogMapper baseMapper;

    @Override
    public void record(String subbarcode, String source, List<CmsFieldChangeVo> changes) {
        if (StringUtils.isBlank(subbarcode) || StringUtils.isBlank(source) || changes == null || changes.isEmpty()) {
            return;
        }
        List<CmsSampleChangeLog> rows = new ArrayList<>(changes.size());
        for (CmsFieldChangeVo change : changes) {
            CmsSampleChangeLog row = new CmsSampleChangeLog();
            row.setSubbarcode(subbarcode);
            row.setSource(source);
            row.setField(change.getField());
            row.setLabel(change.getLabel());
            row.setOldValue(change.getOldValue());
            row.setNewValue(change.getNewValue());
            row.setOperator(change.getBy());
            row.setOperateTime(parseTime(change.getAt()));
            rows.add(row);
        }
        baseMapper.insertBatch(rows);
    }

    @Override
    public List<CmsSampleChangeLogVo> listBySubbarcode(String subbarcode) {
        if (StringUtils.isBlank(subbarcode)) {
            return List.of();
        }
        return baseMapper.selectVoList(Wrappers.<CmsSampleChangeLog>lambdaQuery()
            .eq(CmsSampleChangeLog::getSubbarcode, subbarcode)
            .orderByDesc(CmsSampleChangeLog::getOperateTime)
            .orderByDesc(CmsSampleChangeLog::getId));
    }

    /** 变更项里的时间是 yyyy-MM-dd HH:mm:ss 字符串；解析失败退回当前时间，不因留痕失败影响主流程 */
    private Date parseTime(String at) {
        if (StringUtils.isBlank(at)) {
            return new Date();
        }
        try {
            return Timestamp.valueOf(at);
        } catch (Exception e) {
            log.warn("变更时间格式异常，按当前时间记录：{}", at);
            return new Date();
        }
    }
}
