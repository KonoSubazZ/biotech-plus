# cms_sample 与 CMS 订单查询接口（153 键）字段对照

依据：`药企生信订单查询接口_iPaaS对接说明.md` + `field-dictionary.json`（schemaVersion 1.0）、
`script/sql/business/cms-sample.sql` 的列注释、`CmsSample.java` 实体。
实测库 `ry-vue.cms_sample` = 151 列 = 85 业务列 + 59 deprecated 兼容列 + 7 公共字段。

对账结论：

- 接口一条 orderSamples 记录固定 153 键 = 121 旧字段 + 32 药企扩展；其中 94 键当前有映射，59 键上游固定返回空串。
- 94 个有映射键 → 合并 6 组同源别名（共 9 键）后 87 键；再剔除 CMS 内部主键 `reservnum`/`sampleid`（按口径不建列）→ 85 个业务列。
- 59 个固定空键保留为 `deprecated_*` 列，只存结构，不进实体/VO/BO/Excel/页面。
- 因此接口文档里有 68 个键在页面/接口字段里找不到：59 个保留为空 + 2 个 CMS 内部主键 + 7 个别名键（已并入同名列）。
- 反向：85 个业务列每一列都有接口来源，无凭空字段。

## 1. 85 个业务列 ↔ 接口键

| # | 表列 | 中文（列注释） | 接口键 | 接口中文说明 | 状态 |
|---|---|---|---|---|---|
| 1 | `biotech_cms_batch_id` | CMS订单批次主键 | `abatchid` | 订单批次主键 | 有映射 |
| 2 | `biotech_order_no` | 正式订单编号 | `ORDERCODE` | 正式订单编号 | 有映射 |
| 3 | `biotech_subbarcode` | 诺禾编号 | `BARCODE` | 诺禾编号 | 有映射 |
| 4 | `biotech_batch_no` | 订单批次编号 | `abatchno` | 订单批次编号 | 有映射 |
| 5 | `biotech_business_type` | 订单业务类型 | `businesstype` | 正式订单业务类型 | 有映射 |
| 6 | `biotech_receive_confirm_status` | 订单收样确认状态 | `isconfirm` | 正式订单ISCONFIRM原始状态 | 有映射 |
| 7 | `biotech_customer_code` | 合同甲方客户编码 | `corpno` | 合同甲方客户编码 | 有映射 |
| 8 | `biotech_customer_name` | 正式订单客户名称 | `corpdesc` / `CUSTOMEDESC` / `CUSTOMERNAME` | 正式订单客户显示名称 / 正式订单客户显示名称；与corpdesc、CUSTOMERNAME同取a.DESC78 / 正式订单客户显示名称；与corpdesc、CUSTOMEDESC同取a.DESC78 | 有映射 |
| 9 | `biotech_customer_type` | 客户类型 | `customertype` | 客户类型字典显示值 | 有映射 |
| 10 | `biotech_contract_no` | 关联合同编号 | `contractsno` | 合同编号 | 有映射 |
| 11 | `biotech_contract_name` | 关联合同名称 | `contractname` | 合同名称 | 有映射 |
| 12 | `biotech_scheme_no` | 研究方案编号 | `schemeno` | 批次保存的研究方案编号 | 有映射 |
| 13 | `biotech_project_no` | 药企项目编号 | `yqprojectno` | 批次保存的药企项目编号 | 有映射 |
| 14 | `biotech_research_center` | 研究中心名称 | `researchcenter` | 批次保存的研究中心 | 有映射 |
| 15 | `biotech_department_code` | 合同事业部编码 | `DEPARTMENTCODE` | 合同事业部（部门）编码 | 有映射 |
| 16 | `biotech_contract_sign_date` | 合同签订日期 | `SIGNTIME` | 合同签订日期 | 有映射 |
| 17 | `biotech_payment_term` | 合同账期 | `PAYMENTDATE` | 合同账期原保存字符串；不是格式化的付款日期 | 有映射 |
| 18 | `biotech_checkout_logic` | 合同结算逻辑 | `CHECKOUTLOGIC` | 合同结算逻辑显示值（contract_checkoutlogic字典） | 有映射 |
| 19 | `biotech_contract_block_status` | 合同冻结状态 | `BLOCK` | 合同冻结状态显示值（CRM_csblock字典） | 有映射 |
| 20 | `biotech_contract_contact_email` | 合同联系人邮箱 | `contractemail` | 合同联系人邮箱 | 有映射 |
| 21 | `biotech_operator_email` | 合同运营经理邮箱 | `operatoremail` / `operatemanageremail` | 合同关联运营经理邮箱 / 运营经理邮箱旧别名 | 有映射 |
| 22 | `biotech_customer_account_email` | 合同甲方CSS主账号邮箱 | `corpemail` | 合同甲方公司绑定的CSS主账号邮箱 | 有映射 |
| 23 | `biotech_report_receiver` | 报告接收人 | `REPORTRECEIVER` | 报告接收人 | 有映射 |
| 24 | `biotech_report_email` | 报告接收邮箱 | `reportemail` / `EMAILADDRESS` | 批次保存的报告接收邮箱 / 报告接收邮箱旧别名 | 有映射 |
| 25 | `biotech_report_receiver_phone` | 报告接收人电话 | `RECEIVERTELEPHONE` | 报告接收人电话 | 有映射 |
| 26 | `biotech_report_mailing_address` | 报告邮寄地址 | `MAILINGADDRESS` | 报告邮寄地址 | 有映射 |
| 27 | `biotech_operator_name` | 运营经理名称 | `operatemanagerdesc` | 合同运营经理名称 | 有映射 |
| 28 | `biotech_operator_code` | 运营经理编码 | `operatemanagercode` | 合同运营经理编码 | 有映射 |
| 29 | `biotech_sales_name` | 合同跟进销售名称 | `salesman` | 合同跟进销售名称 | 有映射 |
| 30 | `biotech_sales_code` | 合同跟进销售编码 | `salesmancode` | 合同跟进销售编码 | 有映射 |
| 31 | `biotech_sales_email` | 合同关联销售邮箱 | `salesmanemail` / `SALEREMAIL` | 合同关联销售邮箱 / 合同关联销售邮箱旧键 | 有映射 |
| 32 | `biotech_order_sales_name` | 订单保存的销售名称 | `ERPSALERNAME` | 正式订单保存的销售姓名（旧键ERPSALERNAME，不据键名推断来自ERP） | 有映射 |
| 33 | `biotech_support_email` | 合同技术支持邮箱 | `SUPPORTEMAIL` | 合同关联技术支持邮箱 | 有映射 |
| 34 | `biotech_manager_email` | 合同经理邮箱 | `MANAGEREMAIL` | 合同关联经理邮箱 | 有映射 |
| 35 | `biotech_order_recorder_code` | 订单创建人编码 | `RECORDERCODE` | 正式订单创建人编码 | 有映射 |
| 36 | `biotech_order_recorder_name` | 订单创建人名称 | `recorderdesc` | 正式订单创建人名称 | 有映射 |
| 37 | `biotech_sample_recorder_name` | 样本记录人名称 | `RECORDER` | 样本记录人名称 | 有映射 |
| 38 | `biotech_sales_channel` | 订单销售渠道 | `LIBRARYNAME` | 正式订单销售渠道原保存值（旧键LIBRARYNAME） | 有映射 |
| 39 | `biotech_sales_region` | 订单销售区域 | `FASTCODE` | 正式订单销售区域原保存值（旧键FASTCODE） | 有映射 |
| 40 | `biotech_sender_name` | 送检人 | `sendpeople` | 批次送检人 | 有映射 |
| 41 | `biotech_sender_phone` | 送检人联系电话 | `sendertel` | 批次送检人联系电话 | 有映射 |
| 42 | `biotech_send_date` | 送检日期 | `SENDDATE` | 批次送检日期原保存字符串 | 有映射 |
| 43 | `biotech_express_company` | 样本快递公司 | `expressname` | 样本快递公司显示值（template字典） | 有映射 |
| 44 | `biotech_express_no` | 样本快递单号 | `expressno` | 样本快递单号 | 有映射 |
| 45 | `biotech_transport_condition` | 运输条件 | `transportcondition` | 运输条件显示值 | 有映射 |
| 46 | `biotech_transport_condition_code` | 运输条件 | `transportconditioncode` | 运输条件原存储值 | 有映射 |
| 47 | `biotech_has_temperature_logger` | 是否带温控计 | `hascontroller` | 是否带温控计的原存储字符串 | 有映射 |
| 48 | `biotech_receiving_lab` | 收样实验室 | `laboratoryname` | 正式订单收样实验室 | 有映射 |
| 49 | `biotech_receiving_contact` | 收样联系人 | `samplecontactdesc` | 正式订单收样联系人 | 有映射 |
| 50 | `biotech_receiving_phone` | 收样联系电话 | `samplecontactphone` | 正式订单收样联系电话 | 有映射 |
| 51 | `biotech_receiving_address` | 收样地址 | `sampleaddress` | 正式订单收样地址 | 有映射 |
| 52 | `biotech_customer_sampling_code` | 客户采样识别码 | `customersamplingcode` | 客户采样识别码 | 有映射 |
| 53 | `biotech_sample_name` | 样本名称 | `samplename` | 样本名称 | 有映射 |
| 54 | `biotech_subject_no` | 试验参与者编号 | `subjectid` | 试验参与者编号 | 有映射 |
| 55 | `biotech_sex` | 性别 | `SEX` | 性别显示值 | 有映射 |
| 56 | `biotech_birth_year` | 出生年份 | `birthyear` | 出生年份 | 有映射 |
| 57 | `biotech_disease_type` | 临床诊断或疾病类型 | `diseasetype` | 临床诊断/疾病类型 | 有映射 |
| 58 | `biotech_visit_cycle` | 访视周期 | `visitcycle` | 访视周期 | 有映射 |
| 59 | `biotech_product_code` | 产品编码 | `PCODE` / `sampleproductcode` | 产品编码原保存串 / 产品编码原保存串旧别名 | 有映射 |
| 60 | `biotech_product_name` | 产品名称 | `ERPTESTNAME` | 样本产品名称 | 有映射 |
| 61 | `biotech_sample_type` | 样本类型 | `SAMPLETYPE` | 保存的样本类型名称 | 有映射 |
| 62 | `biotech_sample_type_code` | 样本类型家族码 | `sampletypecode` | 样本类型家族码 | 有映射 |
| 63 | `biotech_sample_quantity` | 样本数量 | `SPECIMENNUM` | 样本数量字符串 | 有映射 |
| 64 | `biotech_quantity_unit` | 数量单位 | `UNIT` / `samplenumunit` | 数量单位显示值 / 数量单位显示值旧别名 | 有映射 |
| 65 | `biotech_sample_attribute` | 样本属性 | `SAMPLEATTRIBUTE` | 样本属性显示值 | 有映射 |
| 66 | `biotech_collection_site` | 取材部位 | `collectionsite` | 取材部位 | 有映射 |
| 67 | `biotech_tissue_type` | 组织类型 | `tissuetype` | 组织类型 | 有映射 |
| 68 | `biotech_tumor_type` | 肿瘤类型 | `tumortype` | 肿瘤类型 | 有映射 |
| 69 | `biotech_primary_tumor_site` | 原发部位 | `primarytumorsite` | 肿瘤原发部位 | 有映射 |
| 70 | `biotech_collection_date` | 采集日期 | `SAMPLETIME` | 样本采集日期 | 有映射 |
| 71 | `biotech_assay_method` | 检测方式 | `assaymethod` | 检测方式 | 有映射 |
| 72 | `biotech_remark` | 样本备注 | `SAMPLEREMARK` | 样本备注 | 有映射 |
| 73 | `biotech_abnormal_remark` | 异常备注 | `ABNORMALREMARK` | 异常备注显示值 | 有映射 |
| 74 | `biotech_control_type` | 管控类型 | `mtacontroltype` | 样本管控类型原保存值 | 有映射 |
| 75 | `biotech_pathology_no` | 病理编号 | `PATHOLOGYNUM` | 样本DESC47原保存值（旧查询别名pathologynum；具体业务含义未获当前VO中文注释确认） | 有映射 |
| 76 | `biotech_pathology_report_paths` | 04类附件路径 | `pathologyreport` | 04类有效附件路径逗号拼接串 | 有映射 |
| 77 | `biotech_other_attachment_paths` | 03类附件路径 | `otherattachments` | 03类有效附件路径逗号拼接串 | 有映射 |
| 78 | `biotech_receive_confirm_time` | 订单收样确认时间 | `COLLECTDATE` | 订单收样确认时间安全投影 | 有映射 |
| 79 | `biotech_order_submit_time` | 订单提交时间 | `ENTERDATE` | 正式订单记录时间 | 有映射 |
| 80 | `biotech_arrival_date` | 到达日期 | `DYDATE` | 样本到达日期 | 有映射 |
| 81 | `biotech_arrival_time` | 到达时间 | `arrivaltime` | 样本到达时间 | 有映射 |
| 82 | `biotech_paid_amount` | 实际支付金额 | `PAYAMOUNT` | 实际支付金额字符串 | 有映射 |
| 83 | `biotech_order_amount` | 订单金额 | `ordermoney` | 订单金额字符串（a.DESC52） | 有映射 |
| 84 | `biotech_payment_serial_no` | 订单流水标识 | `serialnumber` | 正式订单SERIALNUMBER原值（订单VO称“小程序付款编码”，旧生信DTO称“交易流水号”） | 有映射 |
| 85 | `biotech_payment_finish_time` | 付款完成时间 | `payfinishdate` | 正式订单付款完成时间 | 有映射 |

## 2. 同源别名合并（多个接口键写进同一列）

| 表列 | 接口键 | 取值 |
|---|---|---|
| `biotech_customer_name` | `corpdesc` / `CUSTOMEDESC` / `CUSTOMERNAME` | corpdesc=a.DESC78；CUSTOMEDESC=a.DESC78；CUSTOMERNAME=a.DESC78 |
| `biotech_operator_email` | `operatoremail` / `operatemanageremail` | operatoremail=operatoruser.EMAIL；operatemanageremail=operatoruser.EMAIL |
| `biotech_report_email` | `reportemail` / `EMAILADDRESS` | reportemail=b.REPORTEMAIL；EMAILADDRESS=b.REPORTEMAIL |
| `biotech_sales_email` | `salesmanemail` / `SALEREMAIL` | salesmanemail=sale.EMAIL；SALEREMAIL=sale.EMAIL |
| `biotech_product_code` | `PCODE` / `sampleproductcode` | PCODE=s.DESC25；sampleproductcode=s.DESC25 |
| `biotech_quantity_unit` | `UNIT` / `samplenumunit` | UNIT=s.DESC11；samplenumunit=s.DESC11 |

## 3. 接口里存在但不建列的键

| 接口键 | 中文 | 原因 |
|---|---|---|
| `reservnum` | 正式订单主键 | CMS 内部主键；按「拿不到内部主键就不进表」的口径不建列，业务键用 `biotech_subbarcode` |
| `sampleid` | 正式样本主键 | CMS 内部主键；按「拿不到内部主键就不进表」的口径不建列，业务键用 `biotech_subbarcode` |

### 3.1 59 个固定空串兼容键（保留为 deprecated_* 列，不落实体）

| # | 接口键 | 中文说明 |
|---|---|---|
| 1 | `PMEMAIL` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 2 | `FIRSTTREATMENT` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 3 | `SECONDTREATMENT` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 4 | `OUTPATIENT` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 5 | `THIRDTREATMENT` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 6 | `AGE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 7 | `PATIENTPHONE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 8 | `patientinfoemail` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 9 | `SAMPLESOURCE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 10 | `FROMORGAN` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 11 | `patientinfoisacancer` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 12 | `CANCERTYPE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 13 | `cancertype1` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 14 | `cancertypecode` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 15 | `specificcancer` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 16 | `BIRTHDAY` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 17 | `ROOM` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 18 | `BED` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 19 | `SPECIMENNO` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 20 | `admissionhospital` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 21 | `LOCATIONNAME` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 22 | `DOCTORNAME` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 23 | `admissiondoctorphone` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 24 | `admissiondoctoremail` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 25 | `FAMILYFIRST` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 26 | `FAMILYFIRST_CANCERTYPE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 27 | `FAMILYFIRST_CONFIRMTIME` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 28 | `FAMILYFIRST_AGE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 29 | `FAMILYSECOND` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 30 | `familykinshiptwocancer` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 31 | `FAMILYSECOND_CANCERTYPE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 32 | `FAMILYSECOND_CONFIRMTIME` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 33 | `FAMILYSECOND_AGE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 34 | `detectiontime` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 35 | `GENETYPE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 36 | `detectionmethod` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 37 | `testinginstitution` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 38 | `GENERESULT` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 39 | `BIRTHPLACE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 40 | `firsttreatmenttime` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 41 | `firsttreatmentmethod` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 42 | `firsttreatmentdrugregimen` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 43 | `firsttreatmentduration` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 44 | `firsttreatmenteffect` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 45 | `secondtreatmenttime` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 46 | `secondtreatmentmethod` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 47 | `secondtreatmentdrugregimen` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 48 | `secondtreatmentduration` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 49 | `secondtreatmenteffect` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 50 | `thirdtreatmenttime` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 51 | `thirdtreatmentmethod` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 52 | `thirdtreatmentdrugregimen` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 53 | `thirdtreatmentduration` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 54 | `thirdtreatmenteffect` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 55 | `PATIENTNAME` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 56 | `GETSPECDATE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 57 | `CLINICALREMARK` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 58 | `CLINICALSTAGES` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
| 59 | `PATHOLOGICALTYPE` | 旧协议兼容保留字段；当前药企查询未赋值，固定返回空字符串 |
