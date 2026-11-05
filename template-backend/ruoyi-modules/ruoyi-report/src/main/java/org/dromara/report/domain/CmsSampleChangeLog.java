package org.dromara.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.TenantEntity;

import java.util.Date;

/**
 * 样本信息变更流水对象 cms_sample_change_log
 * <p>
 * 一行 = 一个字段的一次变更；追加写，不覆盖。
 * 数据来源只有两类（2026-11-04 用户口径）：
 * <ul>
 *   <li>{@code refresh}：样本信息行内「从CMS更新样本信息」</li>
 *   <li>{@code edit}：查看抽屉里的「编辑」保存</li>
 * </ul>
 * 定时任务（SYSTEM）和 Excel 导入不写本表。
 * <p>
 * 与 {@code cms_sample.sync_log} 的分工：sync_log 只留最近一次（覆盖写，用于刷新弹窗与列表摘要），
 * 本表是历史轨迹（用于抽屉底部「历史修改信息」）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cms_sample_change_log")
public class CmsSampleChangeLog extends TenantEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 诺禾样本编号（刷新会重建行、id 会变，所以按编号关联） */
    private String subbarcode;

    /** 来源：refresh=行内从CMS更新，edit=页面编辑 */
    private String source;

    /** 字段属性名，如 sampleType */
    private String field;

    /** 字段中文名，如 样本类型 */
    private String label;

    /** 旧值 */
    private String oldValue;

    /** 新值 */
    private String newValue;

    /** 操作人（登录名） */
    private String operator;

    /** 操作时间 */
    private Date operateTime;
}
