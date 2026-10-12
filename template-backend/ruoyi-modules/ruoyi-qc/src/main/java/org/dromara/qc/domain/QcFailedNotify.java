package org.dromara.qc.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 样本质控不合格通知（生成记录）对象 qc_failed_notify
 * <p>
 * 注意：实体只用于持久化，不直接作为接口返回值（用 VO）。
 * tenant_id 由租户拦截器在 INSERT 时按当前租户追加（生成走「样本所属药企」的作用域）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("qc_failed_notify")
public class QcFailedNotify extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** 样本编号（= sample_file.subbarcode） */
    private String subbarcode;

    /** 样本信息 id（一定有） */
    private Long sampleFileId;

    /** 湿实验质控 id（湿实验不合格时才有） */
    private Long wetLabQcId;

    /** 生信质控 id（湿实验合格后才可能有） */
    private Long bioinfoQcId;

    /** 质控类型（wet_lab 湿实验 / bioinfo 生信）—— 这条通知是在哪个质控页点处理产生的 */
    private String qcCategory;

    /** 通知单号（样本编号 + 生成时间） */
    private String notifyNo;

    /** 通知状态：已生成（文档）/ 已发送（邮件） */
    private String notifyStatus;

    /** 日期（生成当天 yyyy-MM-dd） */
    private String notifyDate;

    /** 分析时间（质控记录入库时间，自动带出） */
    private String analysisTime;

    /** 检测产品 */
    private String product;

    /** 客户 */
    private String customerName;

    /** 样本类型 */
    private String sampleType;

    /** 不合格环节（多选，顿号分隔的中文名） */
    private String qcStage;

    /** 是否终止检测（多选，顿号分隔的中文名） */
    private String terminateMeasure;

    /** 建议处理办法（多选，顿号分隔的中文名） */
    private String remedy;

    /** 建议处理办法-其他（原文） */
    private String remedyOther;

    /** 不合格原因 */
    private String reason;

    /** 反馈人 */
    private String reporter;

    /** 反馈时间 */
    private String reportTime;

    /** 复核人 */
    private String reviewer;

    /** 复核时间 */
    private String reviewTime;

    /** 渲染用的 JSON 路径 */
    private String jsonPath;

    /** 生成的 DOCX 路径 */
    private String rawDocxPath;

    /** PDF 路径（OnlyOffice 转换产物；转换失败时为空，重新生成可补上） */
    private String pdfPath;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
