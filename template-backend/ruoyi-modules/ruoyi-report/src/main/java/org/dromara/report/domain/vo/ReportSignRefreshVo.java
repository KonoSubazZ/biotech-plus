package org.dromara.report.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 「刷新签名」结果汇总（样本流转 › 报告签名页签的按钮）：批量刷新所有「签名中」报告的签署状态。
 *
 * @author <你的名字>
 */
@Data
public class ReportSignRefreshVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 本次检查的报告条数 */
    private int total;

    /** 刷新成功（签名完成 → 报告待发送）的条数 */
    private int completed;

    /** 签名失败（通道返回失败 / 查询报错）的条数 */
    private int failed;

    /** 仍在签名中的条数 */
    private int signing;

    /** 提示（假实现、逐条失败原因等） */
    private List<String> messages = new ArrayList<>();
}
