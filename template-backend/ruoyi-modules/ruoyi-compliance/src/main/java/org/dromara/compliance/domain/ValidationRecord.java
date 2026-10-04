package org.dromara.compliance.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 3Q 验证记录对象 validation_record
 * <p>
 * 无 del_flag：验证记录是合规上不可篡改的追溯证据，append-only，
 * 见 ai-rules/04-db-schema.md §2「谁加 del_flag」。
 * 实体只用于持久化，不直接作为接口返回值（用 ValidationRecordVo）。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("validation_record")
public class ValidationRecord extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
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
    private Date executedDate;

    /** 结果（passed 通过 / failed 未通过 / na 不适用） */
    private String result;

    /** 验证摘要 */
    private String summary;

    /** 验证文档文件名（原文件名，附件存服务器固定目录） */
    private String fileName;
    // @fields:end
}
