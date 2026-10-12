package org.dromara.qc.domain.bo;

import java.math.BigDecimal;

import io.github.linpeilie.annotations.AutoMapper;
import org.dromara.common.mybatis.core.domain.BaseEntity;
import org.dromara.qc.domain.BioinfoQc;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 生信质控业务对象 bioinfo_qc
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
// mapstruct-plus：生成 Bo → 实体的转换器。Service 里 MapstructUtils.convert(bo, X.class) 靠它，
// 少了这个注解，运行期会抛 ConvertException: cannot find converter from XxxBo to Xxx
@AutoMapper(target = BioinfoQc.class, reverseConvertGenerate = false)
public class BioinfoQcBo extends BaseEntity {

    /** 主键；新增时为空，更新时必填 */
    private Long id;

    // @fields:start
    /** subbarcode（样本编号）；推送进来时为空不算参数错误 —— 服务端按行拒收并给出原因（见 BioinfoQcServiceImpl.receive） */
    @Size(max = 100, message = "subbarcode长度不能超过 100")
    private String subbarcode;

    /** source_id */
    @Size(max = 64, message = "source_id长度不能超过 64")
    private String sourceId;

    /** status（由服务端按阈值判定后写入，推送方不用传） */
    @Size(max = 20, message = "status长度不能超过 20")
    private String status;

    /** bam */
    @Size(max = 255, message = "bam长度不能超过 255")
    private String bam;

    /** gender */
    @Size(max = 10, message = "gender长度不能超过 10")
    private String gender;

    /** lib_id */
    @Size(max = 255, message = "lib_id长度不能超过 255")
    private String libId;

    /** paired */
    @Size(max = 20, message = "paired长度不能超过 20")
    private String paired;

    /** raw_gc */
    private BigDecimal rawGc;

    /** hot_pos */
    @Size(max = 100, message = "hot_pos长度不能超过 100")
    private String hotPos;

    /** raw_q20 */
    private BigDecimal rawQ20;

    /** raw_q30 */
    private BigDecimal rawQ30;

    /** pipeline */
    @Size(max = 50, message = "pipeline长度不能超过 50")
    private String pipeline;

    /** raw_base */
    private BigDecimal rawBase;

    /** ssh_path */
    @Size(max = 500, message = "ssh_path长度不能超过 500")
    private String sshPath;

    /** duplicate */
    private BigDecimal duplicate;

    /** peak_size */
    private BigDecimal peakSize;

    /** qc_result */
    @Size(max = 100, message = "qc_result长度不能超过 100")
    private String qcResult;

    /** sqi16 */
    @Size(max = 50, message = "sqi16长度不能超过 50")
    private String sqi16;

    /** bio_result */
    @Size(max = 50, message = "bio_result长度不能超过 50")
    private String bioResult;

    /** peak_range */
    @Size(max = 50, message = "peak_range长度不能超过 50")
    private String peakRange;

    /** concordance */
    @Size(max = 50, message = "concordance长度不能超过 50")
    private String concordance;

    /** chrall */
    private String chrall;

    /** library_size */
    private BigDecimal librarySize;

    /** bam_realign */
    @Size(max = 255, message = "bam_realign长度不能超过 255")
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
    private BigDecimal coverageTarget50x;

    /** probe_capture_ratio */
    private BigDecimal probeCaptureRatio;

    /** coverage_target_500x */
    private BigDecimal coverageTarget500x;

    /** coverage_target_1000x */
    private BigDecimal coverageTarget1000x;

    /** coverage_uniformity_02xad_on_target */
    private BigDecimal coverageUniformity02xadOnTarget;

    /** before_duplicate */
    private Long beforeDuplicate;
    // @fields:end
}
