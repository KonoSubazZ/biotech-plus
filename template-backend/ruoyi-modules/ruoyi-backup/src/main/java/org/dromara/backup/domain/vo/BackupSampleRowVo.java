package org.dromara.backup.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 样本备份列表行（按 sample_id 聚合 backup_sample_file 得出）
 * <p>
 * 一个样本一行：文件已备/总数、字节数、样本级状态、最近备份时间都在这里算好，前端不再聚合。
 *
 * @author liushangzhi
 */
@Data
@NoArgsConstructor
public class BackupSampleRowVo {

    /** cms_sample.id */
    private Long sampleId;

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

    /** 文件总数 */
    private Integer fileTotal;

    /** 已成功数（含跳过） */
    private Integer fileDone;

    /** 失败数 */
    private Integer fileFailed;

    /** 跳过数（OSS 已有同 MD5） */
    private Integer fileSkipped;

    /** 文件总字节 */
    private Long totalBytes;

    /** 已备份字节 */
    private Long backedBytes;

    /** 样本级状态：0 待备份 / 2 全部成功 / 3 部分成功 / 4 全部失败 */
    private Integer backupStatus;

    /** 最近备份时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastBackupAt;
}
