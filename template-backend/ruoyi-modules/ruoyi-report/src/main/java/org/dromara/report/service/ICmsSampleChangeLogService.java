package org.dromara.report.service;

import org.dromara.report.domain.vo.CmsFieldChangeVo;
import org.dromara.report.domain.vo.CmsSampleChangeLogVo;

import java.util.List;

/**
 * 样本信息变更流水（cms_sample_change_log）
 * <p>
 * 追加式留痕：刷新与编辑各写一次，供查看抽屉底部「历史修改信息」展示。
 *
 * @author <你的名字>
 */
public interface ICmsSampleChangeLogService {

    /**
     * 追加一次操作的变更明细（一项字段一行）。
     *
     * @param subbarcode 诺禾样本编号
     * @param source     来源：refresh / edit
     * @param changes    本次变更（调用方已算好 old/new 与操作人、时间）
     */
    void record(String subbarcode, String source, List<CmsFieldChangeVo> changes);

    /**
     * 按样本编号查历史（时间倒序）。
     *
     * @param subbarcode 诺禾样本编号
     * @return 变更列表；没有则空列表
     */
    List<CmsSampleChangeLogVo> listBySubbarcode(String subbarcode);
}
