package org.dromara.backup.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 样本备份文件明细 backup_sample_file
 * <p>
 * 一物理文件一行；样本级汇总（文件已备/总数、状态）由页面按 sample_id 聚合，不单独建 task 表。
 * 幂等键是 (sample_id, path_hash)，path_hash = md5(server_host + ':' + server_path)。
 *
 * @author liushangzhi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("backup_sample_file")
public class BackupSampleFile extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** sample_file.id */
    private Long sampleId;

    /** analysis_report.report_id（一样本多报告时用于归属） */
    private Long reportId;

    /** 样本编号 */
    private String subbarcode;

    /** 项目编号 */
    private String projectNo;

    /** 订单批次编号 */
    private String orderCode;

    /** 生信产品编码 */
    private String productBio;

    /** 研究中心/客户名称 */
    private String researchCenter;

    /** 文件类别：report_docx、report_pdf、report_json、report_signed_pdf、notify 四类、fastq、bam、vcf、other */
    private String fileCategory;

    /** 文件名（含扩展名） */
    private String fileName;

    /** 文件所在主机 */
    private String serverHost;

    /** 宿主绝对路径 */
    private String serverPath;

    /** md5(host:path)，幂等键 */
    private String pathHash;

    /** 字节数 */
    private Long fileSize;

    /** 源文件 MD5 */
    private String fileMd5;

    /** OSS 对象 key */
    private String ossPath;

    /** OSS 元数据回读值 */
    private String ossMetaMd5;

    /** 0 待备份 / 1 备份中 / 2 成功 / 3 失败 / 4 跳过（OSS 已有同 MD5） */
    private Integer backupStatus;

    /** 备份完成时间 */
    private Date backupTime;

    /** 失败原因 */
    private String failReason;
    // @fields:end
}
