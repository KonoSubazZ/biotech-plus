package org.dromara.report.service.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * cms_sample 的 45 个字段映射（接口键 → 实体属性 → 中文名）。
 * <p>
 * 从 {@code CmsSyncServiceImpl} 抽出来，一是文件太长，二是映射表是"数据结构"不是"逻辑"：
 * 调整字段口径只改这里，同步逻辑不用动。启动时会校验必须是 45 个，缺一个直接启动失败，
 * 避免静默漏字段。
 *
 * @author <你的名字>
 */
final class CmsFieldMapping {

    private CmsFieldMapping() {
    }

    /** 新增样本时 sync_log 里那条变更的 field 取值 */
    static final String INSERT_MARK = "__insert__";

    /** 接口键 → 实体属性（顺序 = cms_sample 列顺序，45 个） */
    static final Map<String, String> API_TO_PROP;

    /** 实体属性 → 中文名（弹窗与 sync_log 共用） */
    static final Map<String, String> PROP_LABEL;

    static {
        Map<String, String> api = new LinkedHashMap<>();
        api.put("abatchno", "batchNo");
        api.put("corpdesc", "customerName");
        api.put("contractname", "contractName");
        api.put("contractsno", "contractNo");
        api.put("schemeno", "schemeNo");
        api.put("yqprojectno", "projectNo");
        api.put("contractemail", "contractContactEmail");
        api.put("operatemanagerdesc", "operatorName");
        api.put("researchcenter", "researchCenter");
        api.put("REPORTRECEIVER", "reportReceiver");
        api.put("reportemail", "reportEmail");
        api.put("sendpeople", "senderName");
        api.put("sendertel", "senderPhone");
        api.put("SENDDATE", "sendDate");
        api.put("expressname", "expressCompany");
        api.put("expressno", "expressNo");
        api.put("transportcondition", "transportCondition");
        api.put("hascontroller", "hasTemperatureLogger");
        api.put("receivetime", "receiveConfirmTime");
        api.put("submittime", "orderSubmitTime");
        api.put("ORDERCODE", "orderNo");
        api.put("BARCODE", "subbarcode");
        api.put("customersamplingcode", "customerSamplingCode");
        api.put("samplename", "sampleName");
        api.put("subjectid", "subjectNo");
        api.put("SEX", "sex");
        api.put("birthyear", "birthYear");
        api.put("diseasetype", "diseaseType");
        api.put("visitcycle", "visitCycle");
        api.put("PCODE", "productCode");
        api.put("ERPTESTNAME", "productName");
        api.put("SAMPLETYPE", "sampleType");
        api.put("SPECIMENNUM", "sampleQuantity");
        api.put("UNIT", "quantityUnit");
        api.put("SAMPLEATTRIBUTE", "sampleAttribute");
        api.put("collectionsite", "collectionSite");
        api.put("tissuetype", "tissueType");
        api.put("tumortype", "tumorType");
        api.put("primarytumorsite", "primaryTumorSite");
        api.put("SAMPLETIME", "collectionDate");
        api.put("assaymethod", "assayMethod");
        api.put("SAMPLEREMARK", "remark");
        api.put("ABNORMALREMARK", "abnormalRemark");
        api.put("mtacontroltype", "controlType");
        api.put("arrivaltime", "arrivalTime");
        API_TO_PROP = Collections.unmodifiableMap(api);

        Map<String, String> label = new LinkedHashMap<>();
        label.put("batchNo", "订单批次编号");
        label.put("customerName", "客户名称");
        label.put("contractName", "合同名称");
        label.put("contractNo", "合同编号");
        label.put("schemeNo", "研究方案编号");
        label.put("projectNo", "项目编号");
        label.put("contractContactEmail", "联系人邮箱");
        label.put("operatorName", "运营经理");
        label.put("researchCenter", "研究中心名称");
        label.put("reportReceiver", "报告接收人");
        label.put("reportEmail", "报告接收邮箱");
        label.put("senderName", "送检人");
        label.put("senderPhone", "联系电话");
        label.put("sendDate", "送检日期");
        label.put("expressCompany", "快递公司");
        label.put("expressNo", "快递单号");
        label.put("transportCondition", "快递条件");
        label.put("hasTemperatureLogger", "是否带温控计");
        label.put("receiveConfirmTime", "收样确认时间");
        label.put("orderSubmitTime", "提交时间");
        label.put("orderNo", "订单编号");
        label.put("subbarcode", "诺禾编号");
        label.put("customerSamplingCode", "客户采样识别码");
        label.put("sampleName", "样本名称");
        label.put("subjectNo", "试验参与者编号");
        label.put("sex", "性别");
        label.put("birthYear", "出生年份");
        label.put("diseaseType", "临床诊断");
        label.put("visitCycle", "访视周期");
        label.put("productCode", "产品编码");
        label.put("productName", "产品名称");
        label.put("sampleType", "样本类型");
        label.put("sampleQuantity", "样本数量");
        label.put("quantityUnit", "数量单位");
        label.put("sampleAttribute", "样本属性");
        label.put("collectionSite", "取材部位");
        label.put("tissueType", "组织类型");
        label.put("tumorType", "肿瘤类型");
        label.put("primaryTumorSite", "原发部位");
        label.put("collectionDate", "采集日期");
        label.put("assayMethod", "检测方式");
        label.put("remark", "备注");
        label.put("abnormalRemark", "异常备注");
        label.put("controlType", "管控类型");
        label.put("arrivalTime", "到达时间");
        if (api.size() != 45 || label.size() != 45) {
            throw new IllegalStateException(
                "cms_sample 字段映射必须是 45 个，当前 api=" + api.size() + " label=" + label.size());
        }
        PROP_LABEL = Collections.unmodifiableMap(label);
    }

}
