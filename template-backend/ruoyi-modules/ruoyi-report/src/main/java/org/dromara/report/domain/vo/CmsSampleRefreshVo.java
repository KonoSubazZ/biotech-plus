package org.dromara.report.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 行内「刷新」的结果。
 * <p>
 * changed=false 表示与 CMS 一致（changes 为空）；changed=true 时 changes 逐字段给出旧值/新值，
 * 同时该数组已原样写进 cms_sample.sync_log。
 *
 * @author <你的名字>
 */
@Data
public class CmsSampleRefreshVo {

    /** 是否有更新 */
    private boolean changed;

    /** 逐字段变更（新增样本时只有一项 __insert__） */
    private List<CmsFieldChangeVo> changes;

    /** 本次操作人 */
    private String by;

    /** 本次操作时间 yyyy-MM-dd HH:mm:ss */
    private String at;

    /** 刷新后的样本行（前端直接替换当前行） */
    private CmsSampleVo sample;
}
