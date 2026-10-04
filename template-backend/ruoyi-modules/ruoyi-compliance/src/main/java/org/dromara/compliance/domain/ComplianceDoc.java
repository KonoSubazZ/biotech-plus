package org.dromara.compliance.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 3Q 文档管理对象 compliance_document
 * <p>
 * 记录类：带 del_flag（@TableLogic），删除是「作废」语义、可恢复，
 * 见 ai-rules/04-db-schema.md §2「谁加 del_flag」（与 append-only 的开发记录/验证记录相反）。
 * 实体只用于持久化，不直接作为接口返回值（用 ComplianceDocVo）。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("compliance_document")
public class ComplianceDoc extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** 文档类型（IQ 安装确认 / OQ 运行确认 / PQ 性能确认 / DEV_TEST 开发测试） */
    private String docType;

    /** 文档标题 */
    private String title;

    /** 版本号 */
    private String version;

    /** 文档文件名（原文件名，附件存服务器固定目录） */
    private String fileName;

    /** 备注 */
    private String remark;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
