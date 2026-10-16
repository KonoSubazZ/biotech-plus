package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 样本流转页签条数（页签标签上的数字）：同时给「与我相关」与「全部」两个口径。
 * <p>
 * 各页签的可见性口径与列表完全一致（同一份 SQL 条件），所以数字点进去一定对得上。
 *
 * @author <你的名字>
 */
@Data
public class SampleFlowTabCountVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 页签名（与前端页签定义、report:flow:* 权限点后缀一致） */
    private String tab;

    /** 与我相关的条数（= 列表默认口径，只看相关开关关掉时看到的数量） */
    private int mineCount;

    /** 全部条数（= 打开「只看相关」时的数量）；与 mineCount 相同表示该页签不区分人 */
    private int allCount;
}
