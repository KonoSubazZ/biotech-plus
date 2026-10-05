package org.dromara.report.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 报告模板对象 report_template
 * <p>
 * 一张模板记录 = 一个 DOCX 实体文件（{@code template_path}）+ 一条「输出范围配置」（{@code module_code}）。
 * {@code module_code} 是分号分隔的**有序**个性化模块编码列表，决定这份模板的 JSON 里追加哪些个性化字段；
 * 空值表示只输出公共字段。DOCX 渲染（docxtpl 注入 JSON）不在本表职责内。
 * <p>
 * 与产品是多对多：关系落在 product_template（product_id → product_config.id）。
 * 注意：实体只用于持久化，不直接作为接口返回值（用 ReportTemplateVo）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("report_template")
public class ReportTemplate extends BaseEntity {

    /** 主键 */
    @TableId(value = "template_id")
    private Long templateId;

    /** 稳定模板编码（业务键，同租户内唯一） */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 模板版本 */
    private String templateVersion;

    /** 客户编码 */
    private String customerCode;

    /** 报告类型 */
    private String reportType;

    /** 有序个性化模块编码列表（分号分隔）；空 = 只输出公共字段 */
    private String moduleCode;

    /** 报告命名模板：静态文本 + {{路径}} 动态取值；空 = 用默认命名 */
    private String reportName;

    /** DOCX 模板文件路径（预留：相对受控目录或绝对路径） */
    private String templatePath;

    /** 模板文件 SHA-256（预留，用于校验模板是否被替换） */
    private String templateSha256;

    /** 状态（ENABLED / DISABLED） */
    private String status;

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
