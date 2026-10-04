package org.dromara.compliance.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 开发记录对象 dev_log
 * <p>
 * 无 del_flag：开发记录是合规上不可篡改的追溯证据，append-only，
 * 见 ai-rules/04-db-schema.md §2「谁加 del_flag」。
 * 实体只用于持久化，不直接作为接口返回值（用 DevLogVo）。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dev_log")
public class DevLog extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
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

    /** 记录文档文件名（原文件名，附件存服务器固定目录） */
    private String fileName;

    /** 开发人员 */
    private String developer;

    /** 记录日期 */
    private Date logDate;
    // @fields:end
}
