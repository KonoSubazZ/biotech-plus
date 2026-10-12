package org.dromara.qc.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 湿实验质控数据接收入参（上游湿实验流程批量推送）
 * <p>
 * 一行 = 一个样本（与 wet_lab_qc 表一一对应，见 {@link WetLabQcBo}）。
 * {@code subbarcode} 由服务端逐行校验：靠它去 sample_file 取样本自身的 tenant_id 作为落库租户，
 * 调用方不用也不允许指定租户。只有部分质控项有值时，给哪列写哪列，其余列保留库里原值。
 * <p>
 * 与生信质控（BioinfoQcReceiveBo）同一套写法。
 *
 * @author <你的名字>
 */
@Data
public class WetLabQcReceiveBo {

    /** 待落库的质控记录（一行一个样本） */
    @Valid
    @NotEmpty(message = "items 不能为空")
    private List<WetLabQcBo> items;
}
