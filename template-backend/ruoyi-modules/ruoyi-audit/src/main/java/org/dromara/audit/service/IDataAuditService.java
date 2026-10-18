package org.dromara.audit.service;

import org.dromara.audit.domain.bo.DataAuditBo;
import org.dromara.audit.domain.vo.DataAuditDetailVo;
import org.dromara.audit.domain.vo.DataAuditVo;
import org.dromara.common.mybatis.core.page.PageQuery;
import org.dromara.common.mybatis.core.page.TableDataInfo;

public interface IDataAuditService {
    TableDataInfo<DataAuditVo> selectPageList(DataAuditBo bo, PageQuery pageQuery);
    DataAuditDetailVo queryById(Long id);
}
