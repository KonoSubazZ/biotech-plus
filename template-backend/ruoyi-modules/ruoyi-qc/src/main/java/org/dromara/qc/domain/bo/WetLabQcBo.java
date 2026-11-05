package org.dromara.qc.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.qc.domain.WetLabQc;

/**
 * 湿实验质控业务对象 wet_lab_qc（一行一个样本）
 * <p>
 * 两个用途：① 页面检索条件（目前只用 subbarcode）；② 接口推送的**一行样本**（见 WetLabQcReceiveBo）。
 * 与生信质控同一套写法（BioinfoQcBo 也既是检索条件又是推送行）。
 * <p>
 * 推送时字段留空不算参数错误 —— 服务端按行处理：没给值的列保留库里的原值（合并写入），
 * 整行定位不到才拒收并给出原因。
 *
 * @author <你的名字>
 */
@Data
// mapstruct-plus：生成 Bo → 实体的转换器（Service 里 MapstructUtils.convert 靠它）
@AutoMapper(target = WetLabQc.class, reverseConvertGenerate = false)
public class WetLabQcBo {

    /** 主键；新增时为空 */
    private Long id;

    /** 样本条码（关联 cms_sample.subbarcode） */
    @Size(max = 80, message = "subbarcode 长度不能超过 80")
    private String subbarcode;

    // @fields:start
    /** 肿瘤纯度 */
    @Size(max = 50, message = "patho_tumor_purity 长度不能超过 50")
    private String pathoTumorPurity;

    /** DNA总量（ng） */
    @Size(max = 50, message = "dna_total_ng 长度不能超过 50")
    private String dnaTotalNg;

    /** 检测结果判断（等级 A/B/C/D） */
    @Size(max = 50, message = "dna_degradation 长度不能超过 50")
    private String dnaDegradation;

    /** 预文库总量（ng） */
    @Size(max = 50, message = "pre_lib_total_ng 长度不能超过 50")
    private String preLibTotalNg;

    /** 终文库浓度（ng/uL） */
    @Size(max = 50, message = "final_lib_conc 长度不能超过 50")
    private String finalLibConc;

    /** DNA总量（ng）对照 */
    @Size(max = 50, message = "dna_total_ng_normal 长度不能超过 50")
    private String dnaTotalNgNormal;

    /** 预文库总量（ng）对照 */
    @Size(max = 50, message = "pre_lib_total_ng_normal 长度不能超过 50")
    private String preLibTotalNgNormal;

    /** 终文库浓度（ng/uL）对照 */
    @Size(max = 50, message = "final_lib_conc_normal 长度不能超过 50")
    private String finalLibConcNormal;
    // @fields:end

    /** 质控状态（页面检索用：passed / failed / pending）；推送与修改时由服务端重判，传了也不算数 */
    @Size(max = 20, message = "status 长度不能超过 20")
    private String status;
}
