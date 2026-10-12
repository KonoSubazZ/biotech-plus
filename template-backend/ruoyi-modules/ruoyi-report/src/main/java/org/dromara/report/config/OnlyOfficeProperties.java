package org.dromara.report.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OnlyOffice 文档转换配置（onlyoffice.*）。
 * <p>
 * 默认值按「Document Server 起在本机 18098、后端与它同宿主」设计。部署时用环境变量覆盖，
 * 最要紧的是 {@code ONLYOFFICE_PUBLIC_BASE}：Document Server 只能按 URL 去取待转换的源文件，
 * 这个值必须是「Document Server 能访问到的后端地址」，否则转换一定失败。
 * <p>
 * {@code public-base} 留空 = 不做转换（通知单照常生成，只记一条 warn），不猜地址、不兜底。
 *
 * @author <你的名字>
 */
@Data
@Component
@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeProperties {

    /** 是否启用 DOCX → PDF 转换 */
    private boolean enabled = true;

    /** Document Server 地址（后端直连它的地址） */
    private String dsUrl = "http://127.0.0.1:18098";

    /** 后端对 Document Server 可达的基址（环境变量 ONLYOFFICE_PUBLIC_BASE）；留空 = 不转换 */
    private String publicBase;

    /** 单次转换超时（秒） */
    private long timeoutSeconds = 180;

    /** 源文件临时读取凭证的有效期（秒）；Document Server 取走即可，过期自动失效 */
    private long sourceTtlSeconds = 600;

    /** 是否校验产物带 %PDF- 文件头 */
    private boolean verifyPdf = true;
}
