# cms_sample 字段对照（CMS 原始设计 45 字段 ↔ 接口键 ↔ 表列）

依据：原始设计字段清单（`MLS_APPOINTMENTBATCH` / `MLS_SAMPLEINFO` / `MLS_CONTRACT`）+
`药企生信订单查询接口_iPaaS对接说明.md` 的实测返回（45 键）。

2026-11-03 重构口径：

- 表从 151 列（85 业务 + 59 `deprecated_*` + 7 公共）重建为 **52 列**（45 业务 + id + 6 公共）。
- 列名去掉 `biotech_` 前缀（前缀原用于区分废弃列，废弃列已全部删除）。
- 只保留接口现在仍返回的 45 个键；上游固定返回空串的 59 键、以及接口已不再返回的 39 个业务列全部删除。
- 业务键：`uk_cms_sample_subbarcode(tenant_id, subbarcode)`。

## 1. 45 个字段对应

| # | 设计字段 | CMS 表.列 | 接口键 | 表列 | 类型 |
|---|---|---|---|---|---|
| 1 | 订单批次编号 | MLS_APPOINTMENTBATCH.ABATCHNO | abatchno | batch_no | varchar(128) |
| 2 | 客户名称 | MLS_APPOINTMENTBATCH.CORPDESC | corpdesc | customer_name | varchar(100) |
| 3 | 合同名称 | MLS_APPOINTMENTBATCH.CONTRACTNAME | contractname | contract_name | varchar(100) |
| 4 | 合同编号 | MLS_APPOINTMENTBATCH.CONTRACTCODE | contractsno ⚠ | contract_no | varchar(128) |
| 5 | 研究方案编号 | MLS_APPOINTMENTBATCH.SCHEMENO | schemeno | scheme_no | varchar(128) |
| 6 | 项目编号 | MLS_APPOINTMENTBATCH.YQPROJECTNO | yqprojectno | project_no | varchar(128) |
| 7 | 联系人邮箱 | MLS_APPOINTMENTBATCH.CONTACTEMAIL | contractemail | contract_contact_email | varchar(100) |
| 8 | 送检人 | MLS_APPOINTMENTBATCH.SENDPEOPLE | sendpeople | sender_name | varchar(100) |
| 9 | 联系电话 | MLS_APPOINTMENTBATCH.SENDERTEL | sendertel | sender_phone | varchar(100) |
| 10 | 送检日期 | MLS_APPOINTMENTBATCH.SENDDATE | SENDDATE | send_date | varchar(10) |
| 11 | 快递公司 | MLS_APPOINTMENTBATCH.DESC6 | expressname | express_company | varchar(100) |
| 12 | 快递单号 | MLS_APPOINTMENTBATCH.DESC7 | expressno | express_no | varchar(100) |
| 13 | 快递条件 | MLS_APPOINTMENTBATCH.DESC8 | transportcondition | transport_condition | varchar(100) |
| 14 | 是否带温控计 | MLS_APPOINTMENTBATCH.HASCONTROLLER | hascontroller | has_temperature_logger | varchar(16) |
| 15 | 研究中心名称 | MLS_APPOINTMENTBATCH.RESEARCHCENTER | researchcenter | research_center | varchar(100) |
| 16 | 报告接收人 | MLS_APPOINTMENTBATCH.REPORTRECEIVER | REPORTRECEIVER | report_receiver | varchar(100) |
| 17 | 报告接收邮箱 | MLS_APPOINTMENTBATCH.REPORTEMAIL | reportemail | report_email | varchar(100) |
| 18 | 收样确认时间 | MLS_APPOINTMENTBATCH.AUDITTIME | receivetime ⚠ | receive_confirm_time | varchar(19) |
| 19 | 提交时间（订单） | MLS_APPOINTMENTBATCH.SUBMITTIME | submittime | order_submit_time | varchar(19) |
| 20 | 订单编号 | MLS_SAMPLEINFO.DESC50 | ORDERCODE | order_no | varchar(128) |
| 21 | 诺禾编号 | MLS_SAMPLEINFO.DESC3 | BARCODE | subbarcode | varchar(128) NOT NULL |
| 22 | 客户采样识别码 | MLS_SAMPLEINFO.DESC24 | customersamplingcode | customer_sampling_code | varchar(100) |
| 23 | 样本名称 | MLS_SAMPLEINFO.DESC23 | samplename | sample_name | varchar(100) |
| 24 | 试验参与者编号 | MLS_SAMPLEINFO.CUSTOMEREXPNUM | subjectid ⚠ | subject_no | varchar(100) |
| 25 | 性别 | MLS_SAMPLEINFO.DESC12 | SEX | sex | varchar(16) |
| 26 | 出生年份 | MLS_SAMPLEINFO.BIRTH | birthyear | birth_year | char(4) |
| 27 | 临床诊断 | MLS_SAMPLEINFO.desc17 | diseasetype | disease_type | varchar(100) |
| 28 | 访视周期 | MLS_SAMPLEINFO.desc30 | visitcycle | visit_cycle | varchar(100) |
| 29 | 产品编码 | MLS_SAMPLEINFO.DESC25 | PCODE | product_code | varchar(100) |
| 30 | 产品名称 | MLS_SAMPLEINFO.DESC2 | ERPTESTNAME | product_name | varchar(100) |
| 31 | 样本类型 | MLS_SAMPLEINFO.DESC1 | SAMPLETYPE | sample_type | varchar(100) |
| 32 | 样本数量 | MLS_SAMPLEINFO.DESC4 | SPECIMENNUM | sample_quantity | varchar(32) |
| 33 | 数量单位 | MLS_SAMPLEINFO.DESC11 | UNIT | quantity_unit | varchar(32) |
| 34 | 样本属性 | MLS_SAMPLEINFO.DESC67 | SAMPLEATTRIBUTE | sample_attribute | varchar(100) |
| 35 | 取材部位 | MLS_SAMPLEINFO.desc16 | collectionsite | collection_site | varchar(100) |
| 36 | 组织类型 | MLS_SAMPLEINFO.desc18 | tissuetype | tissue_type | varchar(100) |
| 37 | 肿瘤类型 | MLS_SAMPLEINFO.desc19 | tumortype | tumor_type | varchar(100) |
| 38 | 原发部位 | MLS_SAMPLEINFO.desc20 | primarytumorsite | primary_tumor_site | varchar(100) |
| 39 | 采集日期 | MLS_SAMPLEINFO.DESC34 | SAMPLETIME | collection_date | varchar(10) |
| 40 | 检测方式 | MLS_SAMPLEINFO.desc21 | assaymethod | assay_method | varchar(100) |
| 41 | 备注 | MLS_SAMPLEINFO.REMARK | SAMPLEREMARK | remark | varchar(500) |
| 42 | 异常备注 | MLS_SAMPLEINFO.DESC100 | ABNORMALREMARK | abnormal_remark | text |
| 43 | 管控类型 | MLS_SAMPLEINFO.MTACONTROLTYPE | mtacontroltype | control_type | varchar(100) |
| 44 | 到达时间 | MLS_SAMPLEINFO.DYDATE | arrivaltime | arrival_time | varchar(19) |
| 45 | 运营经理 | MLS_CONTRACT.OPERATEMANAGERDESC | operatemanagerdesc | operator_name | varchar(100) |

> ⚠ 三处接口键与设计列名不同：`contractsno` ↔ CONTRACTCODE、`receivetime` ↔ AUDITTIME、
> `subjectid` ↔ CUSTOMEREXPNUM；取值按接口键。

## 2. 其他业务用到的列（改动时需同步）

| 使用方 | 位置 | 用到的列 |
|---|---|---|
| 质控不合格通知预填 | `ruoyi-qc/.../QcFailedNotifyMapper.xml` | subbarcode / contract_no / order_no / subject_no / receive_confirm_time / sample_type / sample_quantity / product_name / customer_name；center 暂留空 |
| 质控归属药企 | `QcFailedNotifyMapper.xml` / `WetLabQcMapper.xml` / `BioinfoQcMapper.xml` | subbarcode / tenant_id |
| 干实验质控客户名 | `BioinfoQcMapper.xml` | customer_name |
| 样本流转列表 | `SampleFlowMapper.xml` | customer_name / sample_name / sample_type / product_name / subbarcode |
| 样本自身手写 SQL | `CmsSampleMapper.xml` | subbarcode |
| 报告 LIMS 信息 | `InterpretationServiceImpl` | subject_no / sample_name / sex / birth_year / disease_type / abnormal_remark / customer_name / sample_type / sample_quantity / quantity_unit / collection_site / collection_date / arrival_time（取前 10 位）/ order_submit_time / product_name / remark / report_receiver / report_email |
