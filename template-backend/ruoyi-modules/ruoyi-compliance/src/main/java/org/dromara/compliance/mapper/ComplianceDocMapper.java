package org.dromara.compliance.mapper;

import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;
import org.dromara.compliance.domain.ComplianceDoc;
import org.dromara.compliance.domain.vo.ComplianceDocVo;

/**
 * 3Q 文档管理 数据层
 * <p>
 * 单表增删改查、简单条件组合、排序、计数一律用继承来的 BaseMapperPlus 能力，
 * 不写自定义 SQL。删除走 @TableLogic（软删）。
 *
 * @author liushangzhi
 */
public interface ComplianceDocMapper extends BaseMapperPlus<ComplianceDoc, ComplianceDocVo> {
}
