package org.dromara.backup.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 样本备份文件明细（抽屉里的一行）
 *
 * @author liushangzhi
 */
@Data
@NoArgsConstructor
public class BackupSampleFileVo {

    /** 主键 */
    private Long id;

    /** 关联报告（可为空） */
    private Long reportId;

    /** 文件类别 */
    private String fileCategory;

    /** 文件名 */
    private String fileName;

    /** 文件所在主机 */
    private String serverHost;

    /** 宿主绝对路径 */
    private String serverPath;

    /** 字节数 */
    private Long fileSize;

    /** 源文件 MD5 */
    private String fileMd5;

    /** OSS 对象 key */
    private String ossPath;

    /** OSS 元数据回读值 */
    private String ossMetaMd5;

    /** 0 待备份 / 1 备份中 / 2 成功 / 3 失败 / 4 跳过 */
    private Integer backupStatus;

    /** 备份完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date backupTime;

    /** 失败原因 */
    private String failReason;
}
