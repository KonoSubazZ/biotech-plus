package org.dromara.drive.utils;

import java.util.regex.Pattern;

/**
 * 文本处理工具：与旧 Python 驱动的解析口径逐字对齐，避免"看起来一样、结果不同"。
 * <ul>
 *   <li>{@link #splitTabs(String)}：Python 的 {@code str.split("\t")} 保留尾部空字段，
 *       Java 的 {@code split("\t")} 默认丢弃 —— 必须用 {@code limit=-1} 才等价；</li>
 *   <li>{@link #removeInterzone(String)}：移植 {@code core/utils.py} 的 remove_interzone；</li>
 *   <li>{@link #formatFrequency(String)}：移植 {@code parse_freq_value}（整数不带小数、小数两位）。</li>
 * </ul>
 *
 * @author <你的名字>
 */
public final class DriveTexts {

    /** 数值形态（含科学计数法）——与 Python float() 能接受的常见写法对齐 */
    private static final Pattern NUMERIC = Pattern.compile("[-+]?(\\d+\\.?\\d*|\\.\\d+)([eE][-+]?\\d+)?");

    private DriveTexts() {
    }

    /**
     * 按制表符切分并保留尾部空字段（等价 Python {@code line.strip().split("\t")}）。
     *
     * @param line 原始行
     * @return 字段数组；空行返回长度 1 的数组
     */
    public static String[] splitTabs(String line) {
        String text = line == null ? "" : line.strip();
        return text.split("\t", -1);
    }

    /**
     * 只保留 del / dup 关键词及其之前的内容；同时含 del 与 ins 时保留 del 段 + ins 起的后半段。
     *
     * @param value 原始 cHGVS
     * @return 处理后的字符串
     */
    public static String removeInterzone(String value) {
        if (value == null) {
            return null;
        }
        int delIndex = value.indexOf("del");
        int insIndex = value.indexOf("ins");
        if (delIndex >= 0 && insIndex >= 0) {
            return value.substring(0, delIndex + 3) + value.substring(insIndex);
        }
        for (String keyword : new String[]{"del", "dup"}) {
            int index = value.indexOf(keyword);
            if (index >= 0) {
                return value.substring(0, index + 3);
            }
        }
        return value;
    }

    /**
     * 丰度/频率格式化：整数不带小数点，小数保留两位；非法或空返回 null。
     *
     * @param raw 原始单元格
     * @return 字符串或 null
     */
    public static String formatFrequency(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String text = raw.strip();
        if (!NUMERIC.matcher(text).matches()) {
            // 非数值（如 "-"、空串、"NA"）按旧脚本口径一律当缺失，不抛错、不影响整行入库
            return null;
        }
        double number = Double.parseDouble(text);
        double rounded = Math.floor(number * 100 + 0.5) / 100;
        if (rounded == Math.rint(rounded)) {
            return String.valueOf((long) rounded);
        }
        return String.format("%.2f", rounded);
    }

    /**
     * 截断文本（analysis_file_status.file_text 是 LONGTEXT，但没必要把超大文件整段塞库）。
     *
     * @param text  原文
     * @param limit 最大字符数
     * @return 截断后的文本
     */
    public static String truncate(String text, int limit) {
        if (text == null) {
            return null;
        }
        if (limit <= 0 || text.length() <= limit) {
            return text;
        }
        return text.substring(0, limit);
    }

    /**
     * 截断错误信息到 1990 字符（analysis_file_status.message 是 varchar(2000)）。
     *
     * @param message 原始错误
     * @return 截断后的错误
     */
    public static String truncateMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= 1990 ? message : message.substring(0, 1990) + "...";
    }
}
