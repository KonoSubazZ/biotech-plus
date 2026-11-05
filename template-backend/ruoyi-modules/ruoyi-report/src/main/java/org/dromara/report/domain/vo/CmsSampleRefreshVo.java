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

    /**
     * 同一订单下的其他样本（双样本：组织 / 血液两条编号不同、订单编号相同）。
     * <p>
     * 点击某一条刷新时，按它的订单编号再查一次 CMS，把同订单其余样本也一次刷掉；
     * 每条各自带 changed / changes，未变化的样本 changed=false、changes 为空。
     * 单样本订单这里为空数组。
     */
    private List<CmsSampleRefreshVo> siblings;
}
