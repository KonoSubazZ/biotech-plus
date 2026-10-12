package org.dromara.qc.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.qc.domain.WetLabQc;

/**
 * 湿实验质控视图对象 wet_lab_qc（一行一个样本）
 *
 * @author <你的名字>
 */
@Data
@NoArgsConstructor
// mapstruct-plus：生成 Vo ↔ 实体的双向转换器（查询结果 Entity → Vo 靠它）
@AutoMapper(target = WetLabQc.class)
public class WetLabQcVo {

    /** 主键 */
    private Long id;

    /** 样本条码 */
    private String subbarcode;

    // @fields:start
    /** 肿瘤纯度（列名 patho_tumor_purity） */
    private String pathoTumorPurity;

    /** DNA总量（ng）（列名 dna_total_ng） */
    private String dnaTotalNg;

    /** 检测结果判断（等级 A/B/C/D）（列名 dna_degradation） */
    private String dnaDegradation;

    /** 预文库总量（ng）（列名 pre_lib_total_ng） */
    private String preLibTotalNg;

    /** 终文库浓度（ng/uL）（列名 final_lib_conc） */
    private String finalLibConc;

    /** DNA总量（ng）对照（列名 dna_total_ng_normal） */
    private String dnaTotalNgNormal;

    /** 预文库总量（ng）对照（列名 pre_lib_total_ng_normal） */
    private String preLibTotalNgNormal;

    /** 终文库浓度（ng/uL）对照（列名 final_lib_conc_normal） */
    private String finalLibConcNormal;
    // @fields:end

    /** 质控状态（passed 合格 / failed 不合格 / pending 待判定） */
    private String status;

    /** 通知发送状态（已生成（文档）/ 已发送（邮件））—— 暂不在页面展示，接口先带上 */
    private String notifyStatus;

    /** 创建时间（= 接口推送落库时间） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
