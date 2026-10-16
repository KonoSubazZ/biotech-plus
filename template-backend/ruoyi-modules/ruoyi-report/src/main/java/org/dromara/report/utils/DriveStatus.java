package org.dromara.report.utils;

/**
 * 样本驱动状态（{@code analysis_data.drive_status}，int 码）。
 * <p>
 * 由外部驱动程序（{@code report-driver/scanner.py}）写：一个样本的文件全部驱动成功才算
 * {@link #SUCCESS}，只要还有失败文件就是 {@link #FAILED}（含"部分成功"）；重新驱动过程写 {@link #DRIVING}。
 *
 * @author <你的名字>
 */
public final class DriveStatus {

    /** 10 驱动失败（含部分文件失败） */
    public static final int FAILED = 10;

    /** 11 驱动成功（该样本所有文件都驱动成功） */
    public static final int SUCCESS = 11;

    /** 12 驱动中（初次驱动或重新驱动执行中） */
    public static final int DRIVING = 12;

    private DriveStatus() {
    }
}
