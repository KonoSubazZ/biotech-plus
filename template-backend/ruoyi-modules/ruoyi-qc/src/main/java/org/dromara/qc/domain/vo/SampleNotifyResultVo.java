package org.dromara.qc.domain.vo;

import lombok.Data;

/**
 * 样本通知生成结果（返回给前端）
 *
 * @author <你的名字>
 */
@Data
public class SampleNotifyResultVo {

    /** 记录 id（sample_notify.id） */
    private Long notifyId;

    /** 通知单号 */
    private String notifyNo;

    /** 渲染用的 JSON 路径 */
    private String jsonPath;

    /** 生成的 DOCX 文件名 */
    private String docxName;

    /** 生成的 DOCX 路径 */
    private String docxPath;

    /** 生成的 PDF 路径；转换失败时为空 */
    private String pdfPath;

    /** 生成的 PDF 文件名；转换失败时为空 */
    private String pdfName;

    /** 当前交付件路径（= PDF） */
    private String filePath;

    /** 本次是否覆盖了同一天的旧记录 */
    private Boolean overwritten;

    /** DOCX 字节数 */
    private Object outputBytes;

    /** DOCX 内容 sha256 */
    private String sha256;

    /** 渲染耗时（毫秒） */
    private Object elapsedMs;

    /** 渲染日志路径（排障用） */
    private String rendererLog;
}
