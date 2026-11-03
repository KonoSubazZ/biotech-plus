-- 样本信息表 cms_sample：与 CMS pharmaOrderQuery 当前返回的 45 个键一一对应。
-- 依据：原始设计字段清单（MLS_APPOINTMENTBATCH / MLS_SAMPLEINFO / MLS_CONTRACT）。
-- 口径：
--   · 列名不再带 biotech_/deprecated_ 前缀（前缀原用于区分废弃列，废弃列已全部删除）
--   · 业务唯一键 uk_cms_sample_subbarcode(tenant_id, subbarcode)；诺禾编号是业务键
--   · 不建 CMS 内部主键（reservnum / sampleid）
-- 本文件只负责建表（业务表结构），不含业务数据。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cms_sample` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',

  -- 订单 / 客户 / 合同（MLS_APPOINTMENTBATCH）
  `batch_no`                varchar(128) DEFAULT NULL COMMENT '订单批次编号；接口字段 abatchno',
  `customer_name`           varchar(100) DEFAULT NULL COMMENT '客户名称；接口字段 corpdesc',
  `contract_name`           varchar(100) DEFAULT NULL COMMENT '合同名称；接口字段 contractname',
  `contract_no`             varchar(128) DEFAULT NULL COMMENT '合同编号；接口字段 contractsno（设计列 CONTRACTCODE）',
  `scheme_no`               varchar(128) DEFAULT NULL COMMENT '研究方案编号；接口字段 schemeno',
  `project_no`              varchar(128) DEFAULT NULL COMMENT '项目编号；接口字段 yqprojectno',
  `contract_contact_email`  varchar(100) DEFAULT NULL COMMENT '联系人邮箱（合同-甲方）；接口字段 contractemail',
  `operator_name`           varchar(100) DEFAULT NULL COMMENT '运营经理；接口字段 operatemanagerdesc',
  `research_center`         varchar(100) DEFAULT NULL COMMENT '研究中心名称；接口字段 researchcenter',
  `report_receiver`         varchar(100) DEFAULT NULL COMMENT '报告接收人；接口字段 REPORTRECEIVER',
  `report_email`            varchar(100) DEFAULT NULL COMMENT '报告接收邮箱；接口字段 reportemail',
  `sender_name`             varchar(100) DEFAULT NULL COMMENT '送检人；接口字段 sendpeople',
  `sender_phone`            varchar(100) DEFAULT NULL COMMENT '联系电话；接口字段 sendertel',
  `send_date`               varchar(10)  DEFAULT NULL COMMENT '送检日期；接口字段 SENDDATE',
  `express_company`         varchar(100) DEFAULT NULL COMMENT '快递公司；接口字段 expressname（设计列 DESC6）',
  `express_no`              varchar(100) DEFAULT NULL COMMENT '快递单号；接口字段 expressno（设计列 DESC7）',
  `transport_condition`     varchar(100) DEFAULT NULL COMMENT '快递条件；接口字段 transportcondition（设计列 DESC8）',
  `has_temperature_logger`  varchar(16)  DEFAULT NULL COMMENT '是否带温控计；接口字段 hascontroller',
  `receive_confirm_time`    varchar(19)  DEFAULT NULL COMMENT '收样确认时间；接口字段 receivetime（设计列 AUDITTIME）',
  `order_submit_time`       varchar(19)  DEFAULT NULL COMMENT '提交时间；接口字段 submittime',

  -- 样本 / 受试者（MLS_SAMPLEINFO）
  `order_no`                varchar(128) DEFAULT NULL COMMENT '订单编号；接口字段 ORDERCODE（设计列 DESC50）',
  `subbarcode`              varchar(128) NOT NULL COMMENT '诺禾编号；接口字段 BARCODE（设计列 DESC3）',
  `customer_sampling_code`  varchar(100) DEFAULT NULL COMMENT '客户采样识别码；接口字段 customersamplingcode（设计列 DESC24）',
  `sample_name`             varchar(100) DEFAULT NULL COMMENT '样本名称；接口字段 samplename（设计列 DESC23）',
  `subject_no`              varchar(100) DEFAULT NULL COMMENT '试验参与者编号；接口字段 subjectid（设计列 CUSTOMEREXPNUM）',
  `sex`                     varchar(16)  DEFAULT NULL COMMENT '性别；接口字段 SEX（设计列 DESC12）',
  `birth_year`              char(4)      DEFAULT NULL COMMENT '出生年份；接口字段 birthyear（设计列 BIRTH）',
  `disease_type`            varchar(100) DEFAULT NULL COMMENT '临床诊断；接口字段 diseasetype（设计列 desc17）',
  `visit_cycle`             varchar(100) DEFAULT NULL COMMENT '访视周期；接口字段 visitcycle（设计列 desc30）',
  `product_code`            varchar(100) DEFAULT NULL COMMENT '产品编码；接口字段 PCODE（设计列 DESC25）',
  `product_name`            varchar(100) DEFAULT NULL COMMENT '产品名称；接口字段 ERPTESTNAME（设计列 DESC2）',
  `sample_type`             varchar(100) DEFAULT NULL COMMENT '样本类型；接口字段 SAMPLETYPE（设计列 DESC1）',
  `sample_quantity`         varchar(32)  DEFAULT NULL COMMENT '样本数量；接口字段 SPECIMENNUM（设计列 DESC4）',
  `quantity_unit`           varchar(32)  DEFAULT NULL COMMENT '数量单位；接口字段 UNIT（设计列 DESC11）',
  `sample_attribute`        varchar(100) DEFAULT NULL COMMENT '样本属性；接口字段 SAMPLEATTRIBUTE（设计列 DESC67）',
  `collection_site`         varchar(100) DEFAULT NULL COMMENT '取材部位；接口字段 collectionsite（设计列 desc16）',
  `tissue_type`             varchar(100) DEFAULT NULL COMMENT '组织类型；接口字段 tissuetype（设计列 desc18）',
  `tumor_type`              varchar(100) DEFAULT NULL COMMENT '肿瘤类型；接口字段 tumortype（设计列 desc19）',
  `primary_tumor_site`      varchar(100) DEFAULT NULL COMMENT '原发部位；接口字段 primarytumorsite（设计列 desc20）',
  `collection_date`         varchar(10)  DEFAULT NULL COMMENT '采集日期；接口字段 SAMPLETIME（设计列 DESC34）',
  `assay_method`            varchar(100) DEFAULT NULL COMMENT '检测方式；接口字段 assaymethod（设计列 desc21）',
  `remark`                  varchar(500) DEFAULT NULL COMMENT '备注；接口字段 SAMPLEREMARK（设计列 REMARK）',
  `abnormal_remark`         text                     COMMENT '异常备注；接口字段 ABNORMALREMARK（设计列 DESC100）',
  `control_type`            varchar(100) DEFAULT NULL COMMENT '管控类型；接口字段 mtacontroltype',
  `arrival_time`            varchar(19)  DEFAULT NULL COMMENT '到达时间；接口字段 arrivaltime（设计列 DYDATE）',

  -- 公共字段
  `create_by`   bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime   DEFAULT NULL COMMENT '创建时间',
  `update_by`   bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime   DEFAULT NULL COMMENT '更新时间',
  `del_flag`    char(1)    NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id`   varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_cms_sample_subbarcode` (`tenant_id`,`subbarcode`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_order_no` (`tenant_id`,`order_no`),
  KEY `idx_batch_no` (`tenant_id`,`batch_no`),
  KEY `idx_contract_no` (`tenant_id`,`contract_no`),
  KEY `idx_scheme_no` (`tenant_id`,`scheme_no`),
  KEY `idx_project_no` (`tenant_id`,`project_no`),
  KEY `idx_receive_confirm_time` (`tenant_id`,`receive_confirm_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='样本信息（CMS 45 个设计字段 + 公共字段；业务键 tenant_id + subbarcode）';
