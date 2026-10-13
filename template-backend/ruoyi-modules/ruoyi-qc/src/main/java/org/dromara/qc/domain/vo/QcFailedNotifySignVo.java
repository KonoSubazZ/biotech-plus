package org.dromara.qc.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 质控通知送签结果（送签 / 刷新签署状态共用）。
 *
 * @author <你的名字>
 */
@Data
public class QcFailedNotifySignVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 通知记录 id */
    private Long notifyId;

    /** 上上签合同号 */
    private String contractId;

    /** 网关给的状态描述（原样，供页面展示与排查） */
    private String contractStatus;

    /** 是否已签署完成（完成且签名件已落盘才为 true） */
    private Boolean signed;

    /** 签名后 PDF 路径（未完成时为空） */
    private String signedPdfPath;

    /** 签名后 PDF 文件名（未完成时为空） */
    private String signedPdfName;

    /** 通知状态（已送签 / 已签署） */
    private String notifyStatus;
}
