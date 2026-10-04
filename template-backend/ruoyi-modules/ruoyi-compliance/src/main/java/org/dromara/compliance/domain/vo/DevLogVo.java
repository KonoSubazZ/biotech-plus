package org.dromara.compliance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.compliance.domain.DevLog;

import java.util.Date;

/**
 * 开发记录视图对象 dev_log
 *
 * @author liushangzhi
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = DevLog.class)
public class DevLogVo {

    /** 主键 */
    private Long id;

    // @fields:start
    /** 记录标题 */
    private String title;

    /** 分类（feature 功能新增 / fix 缺陷修复 / change 变更调整） */
    private String category;

    /** 版本号 */
    private String version;

    /** 详细内容 */
    private String content;

    /** 记录文档文件名（原文件名，附件存服务器固定目录；不暴露服务器路径） */
    private String fileName;

    /** 开发人员 */
    private String developer;

    /** 记录日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date logDate;
    // @fields:end

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
