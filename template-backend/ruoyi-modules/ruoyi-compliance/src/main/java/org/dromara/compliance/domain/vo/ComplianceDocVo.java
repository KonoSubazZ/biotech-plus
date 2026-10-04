package org.dromara.compliance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.compliance.domain.ComplianceDoc;

import java.util.Date;

/**
 * 3Q 文档管理视图对象 compliance_document
 *
 * @author liushangzhi
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = ComplianceDoc.class)
public class ComplianceDocVo {

    /** 主键 */
    private Long id;

    // @fields:start
    /** 文档类型（IQ 安装确认 / OQ 运行确认 / PQ 性能确认 / DEV_TEST 开发测试） */
    private String docType;

    /** 文档标题 */
    private String title;

    /** 版本号 */
    private String version;

    /** 文档文件名（原文件名，附件存服务器固定目录；不暴露服务器路径） */
    private String fileName;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
