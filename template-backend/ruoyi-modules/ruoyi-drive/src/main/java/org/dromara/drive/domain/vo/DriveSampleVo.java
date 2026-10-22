package org.dromara.drive.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个样本的驱动结果。
 * <p>
 * 字段口径对齐旧驱动脚本 {@code scanner.py} 的返回（status / message / file_count /
 * total_variant_count / file_names），额外带上 analysisId / driveStatus 方便调用方直接落库或跳转。
 * {@code total_variant_count} 是 {@code snp_del+fusion+cnv+cr_all} 四段拼接，与旧脚本一致。
 *
 * @author <你的名字>
 */
@Data
public class DriveSampleVo {

    /** 分析批次 ID（每次上传 / 重新解读都是新的） */
    private Long analysisId;

    /** 上传批次目录名 YYYYMMDDHHMMSS */
    private String analysisBatchDate;

    /** 分析日期 YYYYMMDD（= 批次目录前 8 位） */
    private String analysisDate;

    /** 生信产品编码（= 上传目录第 2 层） */
    private String productBio;

    /** 样本编号 */
    private String subbarcode;

    /** 样本驱动状态：10 失败 / 11 成功 / 12 驱动中 */
    private Integer driveStatus;

    /** success = 全部文件成功；failed = 有文件失败或缺失（含"部分成功"） */
    private String status;

    /** 汇总信息（与旧脚本 message 同口径：驱动完成/失败原因） */
    private String message;

    /** 成功驱动的文件数 */
    private Integer fileCount;

    /** 成功驱动的文件名（逗号分隔） */
    private String fileNames;

    /** 四类变异数：snp_del+fusion+cnv+cr_all */
    private String totalVariantCount;

    /** 逐文件结果 */
    private List<DriveFileVo> files = new ArrayList<>();
}
