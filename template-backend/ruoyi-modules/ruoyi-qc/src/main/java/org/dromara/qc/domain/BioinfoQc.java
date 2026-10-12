package org.dromara.qc.domain;

import java.math.BigDecimal;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生信质控对象 bioinfo_qc
 * <p>
 * 注意：实体只用于持久化，不直接作为接口返回值（用 BioinfoQcVo）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bioinfo_qc")
public class BioinfoQc extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    // @fields:start
    /** subbarcode */
    private String subbarcode;

    /** source_id */
    private String sourceId;

    /** status */
    private String status;

    /** bam */
    private String bam;

    /** gender */
    private String gender;

    /** lib_id */
    private String libId;

    /** paired */
    private String paired;

    /** raw_gc */
    private BigDecimal rawGc;

    /** hot_pos */
    private String hotPos;

    /** raw_q20 */
    private BigDecimal rawQ20;

    /** raw_q30 */
    private BigDecimal rawQ30;

    /** pipeline */
    private String pipeline;

    /** raw_base */
    private BigDecimal rawBase;

    /** ssh_path */
    private String sshPath;

    /** duplicate */
    private BigDecimal duplicate;

    /** peak_size */
    private BigDecimal peakSize;

    /** qc_result */
    private String qcResult;

    /** sqi16 */
    private String sqi16;

    /** bio_result */
    private String bioResult;

    /** peak_range */
    private String peakRange;

    /** concordance */
    private String concordance;

    /** chrall */
    private String chrall;

    /** library_size */
    private BigDecimal librarySize;

    /** bam_realign */
    private String bamRealign;

    /** target_size */
    private Long targetSize;

    /** total_reads */
    private Long totalReads;

    /** mapping_rate */
    private BigDecimal mappingRate;

    /** average_depth */
    private BigDecimal averageDepth;

    /** coverage_target */
    private BigDecimal coverageTarget;

    /** mapped_reads */
    private Long mappedReads;

    /** duplicate_target */
    private BigDecimal duplicateTarget;

    /** contamination_level */
    private BigDecimal contaminationLevel;

    /** coverage_target_50x */
    @TableField("coverage_target_50x")
    private BigDecimal coverageTarget50x;

    /** probe_capture_ratio */
    private BigDecimal probeCaptureRatio;

    /** coverage_target_500x */
    @TableField("coverage_target_500x")
    private BigDecimal coverageTarget500x;

    /** coverage_target_1000x */
    @TableField("coverage_target_1000x")
    private BigDecimal coverageTarget1000x;

    /** coverage_uniformity_02xad_on_target */
    @TableField("coverage_uniformity_02xad_on_target")
    private BigDecimal coverageUniformity02xadOnTarget;

    /** before_duplicate */
    private Long beforeDuplicate;
    // @fields:end

    /** 通知发送状态（已生成（文档）/ 已发送（邮件））；页面暂不展示，由「处理」生成通知时回写 */
    private String notifyStatus;

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
