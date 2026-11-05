package org.dromara.qc.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 样本通知台账（四类通知一张表）对象 sample_notify
 * <p>
 * 一条记录 = 一张通知单，从生成到发送的全过程都写在同一行上：
 * {@code generated 已生成 → (送签/签署) → pending 待处理（提交后）→ sent 发送成功 / failed 发送失败}。
 * <p>
 * 注意：实体只用于持久化，列表/详情走 VO。
 * tenant_id 由租户拦截器在 INSERT 时按当前租户追加（生成走「样本所属药企」的作用域）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sample_notify")
public class SampleNotify extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** 通知单号（首次生成：样本编号-yyyyMMddHHmmss；同日重生成不变） */
    private String notifyNo;

    /** 通知类型（delay 延期通知 / sample 样本不合格 / wet_lab 湿实验不合格 / bioinfo 干实验不合格） */
    private String notifyType;

    /** 通知状态（generated 已生成待提交 / pending 待处理 / sent 发送成功 / failed 发送失败） */
    private String notifyStatus;

    /** 签署状态（none 未送签 / submitted 已送签 / signed 已签署） */
    private String signStatus;

    /** 通知日期/出具日期（生成当天 yyyyMMdd） */
    private String notifyDate;

    /** 样本编号（= cms_sample.subbarcode） */
    private String subbarcode;

    /** 样本名称（快照，用于文件命名） */
    private String sampleName;

    /** 研究方案编号（= cms_sample.scheme_no） */
    private String schemeNo;

    /** 项目编号（= cms_sample.project_no） */
    private String projectNo;

    /** 试验参与者编号（= cms_sample.subject_no） */
    private String subjectNo;

    /** 产品名称（= cms_sample.product_name） */
    private String productName;

    /** 客户 */
    private String customerName;

    /** 样本类型 */
    private String sampleType;

    /** 不合格原因（邮件正文要用） */
    private String reason;

    /** 检测报告延迟原因（delay 专用） */
    private String delayReason;

    /** 预计报告出具时间（delay 专用） */
    private String expectedReportDate;

    /** 渲染用 JSON 路径（容器内绝对路径） */
    private String jsonPath;

    /** 生成的 DOCX 路径 */
    private String rawDocxPath;

    /** PDF 路径（OnlyOffice 转换产物） */
    private String pdfPath;

    /** 签名后 PDF 路径（上上签回传落盘） */
    private String signedPdfPath;

    /** 交付文件名（不含路径；绝对路径见四个 *_path） */
    private String fileName;

    /** 上上签合同号（送签后回填） */
    private String signContractId;

    /** 处理人 = 生成该通知的人（sys_user.user_id） */
    private Long handlerBy;

    /** 处理时间 = 生成时间 */
    private Date handlerTime;

    /** 发送人（sys_user.user_id） */
    private Long senderBy;

    /** 发送时间 */
    private Date senderTime;

    /** 实际收件人（发送时快照，;分隔） */
    private String sendTo;

    /** 实际抄送（发送时快照，;分隔） */
    private String sendCc;

    /** 最近一次发送失败原因（成功时清空） */
    private String sendError;
    // @fields:end

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
