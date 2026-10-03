-- 样本信息表（sample_file）+「报告管理」菜单与权限点位
-- 字段来源：**生产库的录单样本表 myapp_webcrmsample 全量 122 列**（不含主键 id）
--           dump: ~/project/biotech/sql/biotech-structure-20261002.sql
--           （设计文档 docs/context/设计文档/spec.md §1.4 只是它的裁剪子集，字段偏少，已按真实表补全）
-- 列名：只做大小写/下划线规范化（CUSTOMERNAME→customer_name、ERPTESTNAME→erp_test_name …），
--       每个列的 COMMENT 里都写了「源表列 XXX」，可与原表逐列对照。
--       源表列没有 COMMENT，所以除少数能确定含义的列外，中文名留空、以源列名为准（要补中文名给我对照表即可）。
-- 类型映射：varchar(n) 原样；longtext→text（ai-rules/04-db-schema §7）；源表 122 列全是字符型。
-- 补齐项：主键自增、公共字段、del_flag、tenant_id、uk_sample_file_barcode(tenant_id, barcode)、idx_tenant_id。
-- 业务键：barcode（源表列 BARCODE，样本编号）—— 导入按它 upsert；质控记录 qc_record.subbarcode 也指向它
--         （实验室侧把同一个编号叫 subbarcode，两边叫法不同，值是同一个）。
-- 规范：ai-rules/04-db-schema.md     模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 建表（列顺序与源表一致）
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sample_file` (
    id                            bigint        NOT NULL AUTO_INCREMENT  COMMENT '主键',

    age                           varchar(50)  COMMENT '年龄（源表列 AGE）',
    barcode                       varchar(100)  COMMENT '样本编号（源表列 BARCODE）',
    bed                           varchar(100)  COMMENT '床位（源表列 BED）',
    birth_day                     varchar(100)  COMMENT '出生日期（源表列 BIRTHDAY）',
    birthplace                    varchar(100)  COMMENT '源表列 BIRTHPLACE',
    cancer_type                   varchar(256)  COMMENT '录单癌种（源表列 CANCERTYPE）',
    clinical_remark               varchar(100)  COMMENT '临床备注（源表列 CLINICALREMARK）',
    clinical_stages               varchar(100)  COMMENT '临床分期（源表列 CLINICALSTAGES）',
    collect_date                  varchar(100)  COMMENT '收样日期（源表列 COLLECTDATE）',
    custom_desc                   varchar(256)  COMMENT '客户名称（源表列 CUSTOMEDESC）',
    customer_name                 varchar(100)  COMMENT '客户（源表列 CUSTOMERNAME）',
    department_code               varchar(100)  COMMENT '科室编码（源表列 DEPARTMENTCODE）',
    doctor_name                   varchar(100)  COMMENT '医生姓名（源表列 DOCTORNAME）',
    email_address                 varchar(1024)  COMMENT '邮箱（源表列 EMAILADDRESS）',
    enter_date                    varchar(100)  COMMENT '委托日期（源表列 ENTERDATE）',
    erp_saler_name                varchar(100)  COMMENT 'ERP销售（源表列 ERPSALERNAME）',
    erp_test_name                 varchar(256)  COMMENT '录单产品（源表列 ERPTESTNAME）',
    family_first                  varchar(100)  COMMENT '一级亲属患癌情况（源表列 FAMILYFIRST）',
    family_first_age              varchar(100)  COMMENT '一级亲属年龄（源表列 FAMILYFIRST_AGE）',
    family_first_cancer_type      varchar(100)  COMMENT '一级亲属癌种（源表列 FAMILYFIRST_CANCERTYPE）',
    family_first_confirm_time     varchar(100)  COMMENT '一级亲属确诊时间（源表列 FAMILYFIRST_CONFIRMTIME）',
    family_second                 varchar(100)  COMMENT '二级亲属患癌情况（源表列 FAMILYSECOND）',
    family_second_age             varchar(100)  COMMENT '二级亲属年龄（源表列 FAMILYSECOND_AGE）',
    family_second_cancer_type     varchar(100)  COMMENT '二级亲属癌种（源表列 FAMILYSECOND_CANCERTYPE）',
    family_second_confirm_time    varchar(100)  COMMENT '二级亲属确诊时间（源表列 FAMILYSECOND_CONFIRMTIME）',
    fast_code                     varchar(100)  COMMENT '加急编号（源表列 FASTCODE）',
    first_treatment               varchar(100)  COMMENT '第一次治疗史（源表列 FIRSTTREATMENT）',
    from_organ                    varchar(100)  COMMENT '取材部位（源表列 FROMORGAN）',
    gene_result                   text        COMMENT '基因检测结果（源表列 GENERESULT）',
    gene_type                     text        COMMENT '基因型（源表列 GENETYPE）',
    get_spec_date                 varchar(100)  COMMENT '取材日期（源表列 GETSPECDATE）',
    library_name                  varchar(100)  COMMENT '文库编号（源表列 LIBRARYNAME）',
    location_name                 varchar(100)  COMMENT '病区（源表列 LOCATIONNAME）',
    mailing_address               text        COMMENT '邮寄地址（源表列 MAILINGADDRESS）',
    manager_email                 varchar(100)  COMMENT '经理邮箱（源表列 MANAGEREMAIL）',
    order_code                    varchar(100)  COMMENT '订单编号（源表列 ORDERCODE）',
    outpatient                    varchar(100)  COMMENT '门诊号（源表列 OUTPATIENT）',
    pathological_type             varchar(100)  COMMENT '病理类型（源表列 PATHOLOGICALTYPE）',
    pathology_num                 varchar(100)  COMMENT '病理号（源表列 PATHOLOGYNUM）',
    patient_name                  varchar(100)  COMMENT '姓名（源表列 PATIENTNAME）',
    patient_phone                 varchar(100)  COMMENT '患者电话（源表列 PATIENTPHONE）',
    pay_amount                    varchar(100)  COMMENT '收款金额（源表列 PAYAMOUNT）',
    pcode                         varchar(100)  COMMENT '患者编号（源表列 PCODE）',
    pm_email                      varchar(100)  COMMENT '项目经理邮箱（源表列 PMEMAIL）',
    receiver_telephone            varchar(100)  COMMENT '报告接收人电话（源表列 RECEIVERTELEPHONE）',
    recorder                      varchar(100)  COMMENT '录单人（源表列 RECORDER）',
    recorder_code                 varchar(100)  COMMENT '录单人编码（源表列 RECORDERCODE）',
    report_receiver               varchar(256)  COMMENT '报告接收人（源表列 REPORTRECEIVER）',
    room                          varchar(100)  COMMENT '房间（源表列 ROOM）',
    saler_email                   varchar(100)  COMMENT '销售邮箱(ERP)（源表列 SALEREMAIL）',
    sample_remark                 text        COMMENT '样本备注（源表列 SAMPLEREMARK）',
    sample_source                 varchar(100)  COMMENT '样本来源（源表列 SAMPLESOURCE）',
    sample_time                   varchar(100)  COMMENT '采样日期（源表列 SAMPLETIME）',
    sample_type                   varchar(100)  COMMENT '样本类型（源表列 SAMPLETYPE）',
    second_treatment              text        COMMENT '第二次治疗史（源表列 SECONDTREATMENT）',
    send_date                     varchar(100)  COMMENT '寄出日期（源表列 SENDDATE）',
    sex                           varchar(100)  COMMENT '性别（源表列 SEX）',
    specimen_no                   varchar(100)  COMMENT '标本编号（源表列 SPECIMENNO）',
    specimen_num                  varchar(100)  COMMENT '样本数量（源表列 SPECIMENNUM）',
    support_email                 varchar(100)  COMMENT '支持邮箱（源表列 SUPPORTEMAIL）',
    third_treatment               text        COMMENT '第三次治疗史（源表列 THIRDTREATMENT）',
    unit                          varchar(100)  COMMENT '单位（源表列 UNIT）',
    admission_doctor_email        varchar(100)  COMMENT '接诊医生邮箱（源表列 admissiondoctoremail）',
    admission_doctor_phone        varchar(100)  COMMENT '接诊医生电话（源表列 admissiondoctorphone）',
    admission_hospital            text        COMMENT '就诊医院（源表列 admissionhospital）',
    cancer_type_1                 varchar(100)  COMMENT '癌种1（源表列 cancertype1）',
    cancer_type_code              varchar(100)  COMMENT '癌种编码（源表列 cancertypecode）',
    contract_name                 varchar(100)  COMMENT '合同名称（源表列 contractname）',
    contracts_no                  varchar(100)  COMMENT '合同编号（源表列 contractsno）',
    corp_desc                     varchar(100)  COMMENT '单位名称（源表列 corpdesc）',
    corp_no                       varchar(100)  COMMENT '单位编号（源表列 corpno）',
    customer_type                 varchar(100)  COMMENT '客户类型（源表列 customertype）',
    detection_method              text        COMMENT '检测方法（源表列 detectionmethod）',
    detection_time                varchar(100)  COMMENT '检测时间（源表列 detectiontime）',
    express_name                  varchar(100)  COMMENT '快递公司（源表列 expressname）',
    express_no                    varchar(100)  COMMENT '快递单号（源表列 expressno）',
    family_kinship_two_cancer     varchar(100)  COMMENT '二级亲属患癌情况（源表列 familykinshiptwocancer）',
    first_treatment_drug_regimen  text        COMMENT '第一次治疗用药方案（源表列 firsttreatmentdrugregimen）',
    first_treatment_duration      varchar(100)  COMMENT '第一次治疗时长（源表列 firsttreatmentduration）',
    first_treatment_effect        text        COMMENT '第一次治疗疗效（源表列 firsttreatmenteffect）',
    first_treatment_method        text        COMMENT '第一次治疗方式（源表列 firsttreatmentmethod）',
    first_treatment_time          varchar(100)  COMMENT '第一次治疗时间（源表列 firsttreatmenttime）',
    laboratory_name               varchar(100)  COMMENT '实验室（源表列 laboratoryname）',
    operate_manager_code          varchar(100)  COMMENT '运营负责人编码（源表列 operatemanagercode）',
    operate_manager_desc          varchar(100)  COMMENT '运营负责人（源表列 operatemanagerdesc）',
    operate_manager_email         varchar(100)  COMMENT '运营负责人邮箱（源表列 operatemanageremail）',
    order_money                   varchar(100)  COMMENT '订单金额（源表列 ordermoney）',
    other_attachments             text        COMMENT '其它附件（源表列 otherattachments）',
    pathology_report              text        COMMENT '病理报告（源表列 pathologyreport）',
    patient_info_email            varchar(1024)  COMMENT '患者信息邮箱（源表列 patientinfoemail）',
    patient_info_is_cancer        varchar(100)  COMMENT '患者是否肿瘤（源表列 patientinfoisacancer）',
    pay_finish_date               varchar(100)  COMMENT '收款完成日期（源表列 payfinishdate）',
    recorder_desc                 varchar(100)  COMMENT '录单单位（源表列 recorderdesc）',
    sales_man                     varchar(100)  COMMENT '销售（源表列 salesman）',
    sales_man_code                varchar(100)  COMMENT '销售编码（源表列 salesmancode）',
    sales_man_email               varchar(100)  COMMENT '销售邮箱（源表列 salesmanemail）',
    sample_address                text        COMMENT '送样地址（源表列 sampleaddress）',
    sample_contact_desc           varchar(100)  COMMENT '送样联系人（源表列 samplecontactdesc）',
    sample_contact_phone          varchar(100)  COMMENT '送样联系电话（源表列 samplecontactphone）',
    sample_num_unit               varchar(100)  COMMENT '数量单位（源表列 samplenumunit）',
    sample_product_code           varchar(100)  COMMENT '产品编码（源表列 sampleproductcode）',
    second_treatment_drug_regimen  text        COMMENT '第二次治疗用药方案（源表列 secondtreatmentdrugregimen）',
    second_treatment_duration     varchar(100)  COMMENT '第二次治疗时长（源表列 secondtreatmentduration）',
    second_treatment_effect       text        COMMENT '第二次治疗疗效（源表列 secondtreatmenteffect）',
    second_treatment_method       text        COMMENT '第二次治疗方式（源表列 secondtreatmentmethod）',
    second_treatment_time         varchar(100)  COMMENT '第二次治疗时间（源表列 secondtreatmenttime）',
    serial_number                 varchar(100)  COMMENT '流水号（源表列 serialnumber）',
    specific_cancer               varchar(100)  COMMENT '具体癌种（源表列 specificcancer）',
    testing_institution           varchar(100)  COMMENT '送检机构（源表列 testinginstitution）',
    third_treatment_drug_regimen  text        COMMENT '第三次治疗用药方案（源表列 thirdtreatmentdrugregimen）',
    third_treatment_duration      varchar(100)  COMMENT '第三次治疗时长（源表列 thirdtreatmentduration）',
    third_treatment_effect        text        COMMENT '第三次治疗疗效（源表列 thirdtreatmenteffect）',
    third_treatment_method        text        COMMENT '第三次治疗方式（源表列 thirdtreatmentmethod）',
    third_treatment_time          varchar(100)  COMMENT '第三次治疗时间（源表列 thirdtreatmenttime）',
    department_desc               varchar(50)  COMMENT '科室名称（源表列 DEPARTMENTDESC）',
    abnormal_remark               text        COMMENT '异常备注（源表列 ABNORMALREMARK）',
    checkout_logic                varchar(256)  COMMENT '结算渠道（源表列 CHECKOUTLOGIC）',
    dy_date                       varchar(100)  COMMENT '到样日期（源表列 DYDATE）',
    payment_date                  varchar(100)  COMMENT '账期(天)（源表列 PAYMENTDATE）',
    sample_attribute              varchar(256)  COMMENT '样本属性（源表列 SAMPLEATTRIBUTE）',
    sign_time                     varchar(100)  COMMENT '签约时间（源表列 SIGNTIME）',
    block                         varchar(256)  COMMENT '冻结状态（源表列 BLOCK）',

    -- 公共字段（04-db-schema §2；无 create_dept）
    create_by                     bigint                             COMMENT '创建者',
    create_time                   datetime                           COMMENT '创建时间',
    update_by                     bigint                             COMMENT '更新者',
    update_time                   datetime                           COMMENT '更新时间',
    del_flag                      char(1)      NOT NULL DEFAULT '0'  COMMENT '删除标志（0代表存在 1代表删除）',

    -- 租户列：所有业务表必备
    tenant_id                     varchar(20)  NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 样本编号在一个租户内唯一：导入按它「有则更新、无则新增」，唯一键要求 tenant_id 打头（04-db-schema §4）
    UNIQUE KEY uk_sample_file_barcode (tenant_id, barcode)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='样本信息';


-- ============================================================================
-- 2. 菜单：报告管理（一级目录）> 样本信息
--    幂等：每段都用 NOT EXISTS 包一层派生表（MySQL 5.7 不支持相关派生表，用 @变量传值）
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '报告管理', 0, 3, 'report', NULL, 1, 0,
    'M', '0', '0', '', 'local-icon-post', 1, NOW(), '报告管理（目录）'
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = 0 AND path = 'report') AS exists_check);

SET @report_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.report_sample-info', @report_id, 1, 'sample-info', 'report/sample-info/index', 1, 0,
    'C', '0', '0', 'report:sampleInfo:list', 'local-icon-clipboard', 1, NOW(), '样本信息'
FROM (SELECT 1) AS dummy
WHERE @report_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @report_id AND path = 'sample-info') AS exists_check);

SET @menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @report_id AND path = 'sample-info' LIMIT 1);


-- ============================================================================
-- 按钮权限（F 类型）—— 与 @SaCheckPermission 逐字一致
-- 导入单列一个点位（report:sampleInfo:import）：导入会覆盖既有样本，允许单独授权/收权
-- ============================================================================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:query') AS c1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '新增', @menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:add', '#', 1, NOW(), '新增'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:add') AS c2);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:edit') AS c3);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', @menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:remove', '#', 1, NOW(), '删除'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:remove') AS c4);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '导入', @menu_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'report:sampleInfo:import', '#', 1, NOW(), '导入'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
    WHERE parent_id = @menu_id AND perms = 'report:sampleInfo:import') AS c5);


-- ============================================================================
-- 4. 授权给超级管理员角色（sys_role_menu 主键 (role_id, menu_id)，INSERT IGNORE 保证幂等）
-- ============================================================================
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @report_id OR m.menu_id = @menu_id OR m.parent_id = @menu_id;
