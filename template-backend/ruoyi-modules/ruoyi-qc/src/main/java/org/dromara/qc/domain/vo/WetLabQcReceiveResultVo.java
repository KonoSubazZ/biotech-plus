package org.dromara.qc.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 湿实验质控数据接收结果
 * <p>
 * 逐行处理：能定位到药企、产品与启用标准的正常落库（inserted / updated），
 * 定位不到的被拒收，原因逐条列在 skipped 里 —— 不静默丢数据。
 *
 * @author <你的名字>
 */
@Data
public class WetLabQcReceiveResultVo {

    /** 本次收到的行数 */
    private int received;

    /** 新增行数 */
    private int inserted;

    /** 覆盖更新行数 */
    private int updated;

    /** 被拒收的行及原因 */
    private List<SkippedRow> skipped = new ArrayList<>();

    /**
     * 被拒收的行
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkippedRow {

        /** 样本编号；没写就是 null */
        private String subbarcode;

        /** 拒收原因 */
        private String reason;
    }
}
