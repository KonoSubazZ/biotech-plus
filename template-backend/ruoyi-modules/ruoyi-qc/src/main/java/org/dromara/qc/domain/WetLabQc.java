package org.dromara.qc.domain;

import org.dromara.common.mybatis.core.domain.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 湿实验质控对象 wet_lab_qc
 * <p>
 * **宽表：一行一个样本，每个质控项一列**（形态对齐生信质控 bioinfo_qc）。
 * 列名 = 质控标准里的 qc_param（英文原始字段名），中文名在 qc_standard.qc_item 里维护；
 * 阈值（上下限/单位）留在 qc_standard，判定时按 qc_param 取，不在这张表里冗余。
 * 值列统一 String：阈值本身就是字符串，等级值（B）与带比较符号的值都要能原样存。
 * <p>
 * 注意：实体只用于持久化，不直接作为接口返回值（用 WetLabQcVo）。
 *
 * @author <你的名字>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wet_lab_qc")
public class WetLabQc extends BaseEntity {

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 样本条码（关联 sample_file.subbarcode） */
    private String subbarcode;

    // @fields:start
    /** 肿瘤纯度（qc_param = patho_tumor_purity） */
    private String pathoTumorPurity;

    /** DNA总量（ng）（qc_param = dna_total_ng） */
    private String dnaTotalNg;

    /** 检测结果判断（等级 A/B/C/D）（qc_param = dna_degradation） */
    private String dnaDegradation;

    /** 预文库总量（ng）（qc_param = pre_lib_total_ng） */
    private String preLibTotalNg;

    /** 终文库浓度（ng/uL）（qc_param = final_lib_conc） */
    private String finalLibConc;

    /** DNA总量（ng）对照（qc_param = dna_total_ng_normal） */
    private String dnaTotalNgNormal;

    /** 预文库总量（ng）对照（qc_param = pre_lib_total_ng_normal） */
    private String preLibTotalNgNormal;

    /** 终文库浓度（ng/uL）对照（qc_param = final_lib_conc_normal） */
    private String finalLibConcNormal;
    // @fields:end

    /**
     * 质控状态（passed 合格 / failed 不合格 / pending 待判定）—— 服务端按 qc_standard 阈值判定，
     * 不接受调用方指定（接口落库与人工修改后都会重判）。
     */
    private String status;

    /** 通知发送状态（已生成（文档）/ 已发送（邮件））；页面暂不展示，由「处理」生成通知时回写 */
    private String notifyStatus;

    /** 逻辑删除标志 */
    @TableLogic
    private String delFlag;
}
