package org.dromara.report.domain.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 签名回调入参（上上签未对接期间的占位接口用；接真实通道后由通道回调报文替换）。
 *
 * @author <你的名字>
 */
@Data
public class InterpretationSignCallbackBo {

    /** 签名流程号（送签时写入 report_sign_flow_no） */
    @NotBlank(message = "签名流程号不能为空")
    private String flowNo;

    /** 签名是否成功：true 报告待发送 / false 报告签名失败 */
    @NotNull(message = "签名结果不能为空")
    private Boolean success;

    /** 签名后 PDF 下载地址（未对接期可为空） */
    private String signedPdfUrl;

    /** 失败原因（success=false 时填） */
    private String reason;
}
