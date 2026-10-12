package org.dromara.report.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 报告邮件配置 查询对象（「报告管理 › 邮件配置」列表）
 *
 * @author <你的名字>
 */
@Data
public class ReportMailConfigQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 发件邮箱（模糊） */
    private String fromEmail;
}
