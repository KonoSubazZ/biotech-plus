package org.dromara.drive.utils;

/**
 * 样本驱动状态（{@code analysis_data.drive_status}，int 码）。
 * <p>
 * 口径（与旧 Python 驱动 {@code core/file_scanner.py} 一致）：该样本<b>所有</b>文件都驱动成功才是
 * {@link #SUCCESS}，只要还有失败文件就是 {@link #FAILED}（含"部分成功"）；驱动过程写 {@link #DRIVING}。
 *
 * @author <你的名字>
 */
public final class DriveStatus {

    /** 10 驱动失败（含部分文件失败） */
    public static final int FAILED = 10;

    /** 11 驱动成功（该样本所有文件都驱动成功） */
    public static final int SUCCESS = 11;

    /** 12 驱动中 */
    public static final int DRIVING = 12;

    private DriveStatus() {
    }
}
