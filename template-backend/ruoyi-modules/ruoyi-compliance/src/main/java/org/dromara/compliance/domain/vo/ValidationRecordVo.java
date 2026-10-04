package org.dromara.compliance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.compliance.domain.ValidationRecord;

import java.util.Date;

/**
 * 3Q 验证记录视图对象 validation_record
 *
 * @author liushangzhi
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = ValidationRecord.class)
public class ValidationRecordVo {

    /** 主键 */
    private Long id;

    // @fields:start
    /** 验证类型（IQ 安装确认 / OQ 运行确认 / PQ 性能确认） */
    private String validationType;

    /** 验证标题 */
    private String title;

    /** 版本号（本次验证对应的方案/镜像版本） */
    private String version;

    /** 执行人 */
    private String executedBy;

    /** 执行日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date executedDate;

    /** 结果（passed 通过 / failed 未通过 / na 不适用） */
    private String result;

    /** 验证摘要 */
    private String summary;

    /** 验证文档文件名（原文件名，附件存服务器固定目录；不暴露服务器路径） */
    private String fileName;
    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
