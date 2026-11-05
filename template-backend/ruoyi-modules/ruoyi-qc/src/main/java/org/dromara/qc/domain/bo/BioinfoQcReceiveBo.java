package org.dromara.qc.domain.bo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 生信质控数据接收入参（上游生信流程批量推送）
 * <p>
 * 每个元素的字段与 bioinfo_qc 一一对应，{@code subbarcode} 必填 —— 服务端靠它
 * 去 cms_sample 取客户名、再解析出所属药企（tenant_id），调用方不用也不允许指定租户。
 *
 * @author <你的名字>
 */
@Data
public class BioinfoQcReceiveBo {

    /** 待落库的质控记录（一行一个样本） */
    @Valid
    @NotEmpty(message = "items 不能为空")
    private List<BioinfoQcBo> items;
}
