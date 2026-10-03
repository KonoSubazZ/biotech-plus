-- 报告解读链路建表（17 张：16 张来自参考 SQL + file_Chem）+「报告管理 › 报告解读」菜单/权限 + 模板种子
-- ---------------------------------------------------------------------------
-- 参考：docs/context/report_management_schema.sql（参考工程 db/migration 至 V20 的导出，仅作原型）
--       docs/context/report/report-module-design.md（表结构草案 / 接口 / 业务规则）
-- 按本仓规范改造（ai-rules/04-db-schema.md；体检 python3 tools/check_db_schema.py，error 必须 0）：
--   ① 单列主键统一 bigint NOT NULL AUTO_INCREMENT
--      （参考 SQL 里 analysis_report.report_id / data_file_status.file_id 是 int unsigned → 闸门 bad-pk）
--   ② 每张表补 tenant_id varchar(20) NOT NULL DEFAULT '000000' + KEY idx_tenant_id；唯一键前拼 tenant_id
--   ③ 审计列改本仓命名：create_by(bigint) / create_time(datetime) / update_by(bigint) /
--      update_time(datetime) / del_flag(char(1))。scanner 侧同步改造（见 readme）
--   ④ 去掉参考 SQL 全部物理外键（fk_report_analysis / fk_file_analysis / fk_report_email_*）；
--      product_template 改代理主键 id + uk_product_template(tenant_id, product_id, template_id)
--   ⑤ 产品外键语义：product_id → product_config.id（本仓产品主数据，不再建 product 表）
--   ⑥ LIMS 不建表：复用 sample_file（已含 myapp_webcrmsample 全量 122 列）
--      基因不建表：复用 product_gene（gene_id 指向跨库 nkb.ncbi_gene）
-- 幂等：CREATE TABLE IF NOT EXISTS / NOT EXISTS，可反复执行
-- ---------------------------------------------------------------------------

SET NAMES utf8mb4;

-- analysis_data
CREATE TABLE IF NOT EXISTS `analysis_data` (
    `analysis_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分析数据ID',
    `analysis_date` VARCHAR(8) NOT NULL COMMENT '分析日期YYYYMMDD',
    `subbarcode` VARCHAR(80) NOT NULL COMMENT '样本编号',
    `product` VARCHAR(120) NOT NULL COMMENT '产品名称',
    `product_id` bigint DEFAULT NULL COMMENT '逻辑关联product_config.id（本仓产品主数据表）',
    `barcode` VARCHAR(80) DEFAULT NULL COMMENT '患者编号',
    `analyzer` VARCHAR(80) DEFAULT NULL COMMENT '解读人员',
    `drive_status` VARCHAR(20) NOT NULL DEFAULT 'DRIVING' COMMENT '驱动状态',
    `drive_executed_by` VARCHAR(80) DEFAULT NULL COMMENT '驱动执行人',
    `drive_executed_at` DATETIME DEFAULT NULL COMMENT '驱动执行时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`analysis_id`),
    UNIQUE KEY `uk_analysis_data` (tenant_id, `subbarcode`, `product`, `analysis_date`),
    KEY `idx_analysis_date_product` (`analysis_date`, `product`),
    KEY `idx_analysis_product` (`product`),
    INDEX `idx_analysis_product_id` (`product_id`, `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='样本分析数据';

-- analysis_report
CREATE TABLE IF NOT EXISTS `analysis_report` (
    `report_id` bigint NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    `analysis_id` bigint NOT NULL,
    `analysis_date` VARCHAR(8) NOT NULL,
    `subbarcode` VARCHAR(80) NOT NULL COMMENT '样本编号',
    `product` VARCHAR(120) NOT NULL,
    `product_id` bigint DEFAULT NULL COMMENT '产品ID',
    `product_desc` VARCHAR(255) DEFAULT NULL COMMENT '产品描述',
    `analysis_disease_id` bigint DEFAULT NULL COMMENT '解读癌种ID',
    `analysis_disease` VARCHAR(255) DEFAULT NULL,
    `cancer_type` VARCHAR(256) DEFAULT NULL COMMENT 'LIMS癌种（myapp_webcrmsample.CANCERTYPE）',
    `chem_disease` VARCHAR(255) DEFAULT NULL COMMENT '化疗癌种',
    `report_type` VARCHAR(40) DEFAULT NULL COMMENT '报告类型',
    `report_file_raw_path` VARCHAR(1000) DEFAULT NULL COMMENT '原始DOCX报告路径',
    `report_file_path` VARCHAR(255) DEFAULT NULL COMMENT '报告路径',
    `sub_report_file_path` VARCHAR(255) DEFAULT NULL COMMENT '子报告路径',
    `template` VARCHAR(255) DEFAULT NULL COMMENT '报告模板',
    `report_generated_by` VARCHAR(80) DEFAULT NULL COMMENT '报告产出人',
    `report_generated_at` DATETIME DEFAULT NULL COMMENT '报告生成时间',
    `report_checked_by` VARCHAR(80) DEFAULT NULL COMMENT '报告审核人',
    `report_checked_at` DATETIME DEFAULT NULL COMMENT '报告审核时间',
    `report_sent_by` VARCHAR(80) DEFAULT NULL COMMENT '报告发放人',
    `report_sent_at` DATETIME DEFAULT NULL COMMENT '报告发放时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'INTERPRETING',
    `comment` VARCHAR(300) DEFAULT NULL COMMENT '备注',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `template_id` bigint DEFAULT NULL COMMENT '逻辑关联report_template.template_id',
    `report_json_path` VARCHAR(1000) DEFAULT NULL COMMENT '正式生成JSON文件路径',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`report_id`),
    KEY `idx_report_analysis` (`analysis_id`, `del_flag`),
    KEY `idx_report_status` (`status`),
    INDEX `idx_report_template_id` (`template_id`, `status`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='分析报告';

-- analysis_report_email_log
CREATE TABLE IF NOT EXISTS `analysis_report_email_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `trace_id` CHAR(32) NOT NULL COMMENT '邮件发送追踪号',
    `analysis_id` bigint NOT NULL COMMENT '分析数据ID',
    `report_id` bigint NOT NULL COMMENT '报告ID',
    `recipients` VARCHAR(2000) NOT NULL COMMENT '收件人，分号分隔',
    `recipient_count` bigint NOT NULL COMMENT '收件人数',
    `template_code` VARCHAR(80) DEFAULT NULL COMMENT '邮件模板标识',
    `status` VARCHAR(20) NOT NULL COMMENT 'SENDING/SUCCESS/FAILED',
    `provider_message_id` VARCHAR(255) DEFAULT NULL COMMENT '邮件服务商消息ID',
    `failure_reason` VARCHAR(1000) DEFAULT NULL COMMENT '失败原因摘要',
    `triggered_by` VARCHAR(80) NOT NULL COMMENT '触发人',
    `requested_at` DATETIME NOT NULL COMMENT '发起时间',
    `finished_at` DATETIME DEFAULT NULL COMMENT '完成时间',
    `create_by`   bigint    COMMENT '创建者',
    `create_time` datetime  COMMENT '创建时间',
    `update_by`   bigint    COMMENT '更新者',
    `update_time` datetime  COMMENT '更新时间',
    `del_flag`    char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_email_trace` (tenant_id, `trace_id`),
    KEY `idx_report_email_report` (`report_id`, `requested_at`),
    KEY `idx_report_email_analysis` (`analysis_id`, `requested_at`),
    KEY `idx_report_email_status` (`status`, `requested_at`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='报告邮件发送审计日志';

-- data_file_status
CREATE TABLE IF NOT EXISTS `data_file_status` (
    `file_id` bigint NOT NULL AUTO_INCREMENT,
    `file_path` VARCHAR(300) NOT NULL,
    `file_name` VARCHAR(120) NOT NULL,
    `data_type` VARCHAR(40) NOT NULL,
    `file_type` VARCHAR(40) NOT NULL,
    `file_text` LONGTEXT,
    `analysis_date` VARCHAR(8) NOT NULL,
    `analyzer` VARCHAR(80) DEFAULT NULL,
    `barcode` VARCHAR(80) NOT NULL,
    `subbarcode` VARCHAR(80) NOT NULL,
    `product_name` VARCHAR(120) NOT NULL,
    `status` VARCHAR(20) NOT NULL,
    `message` VARCHAR(2000) DEFAULT NULL,
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `mut_num` INT DEFAULT NULL,
    `analysis_id` bigint NOT NULL,
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`file_id`),
    UNIQUE KEY `uk_data_file_status` (tenant_id, `file_path`, `file_name`),
    KEY `idx_report_case` (`analysis_date`, `product_name`, `subbarcode`, `status`),
    KEY `idx_report_file_subbarcode` (`subbarcode`, `analysis_date`),
    KEY `idx_file_analysis` (`analysis_id`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='样本文件驱动状态';

-- file_Somatic_SNV_Indel
CREATE TABLE IF NOT EXISTS `file_Somatic_SNV_Indel` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `chr` varchar(50) DEFAULT NULL COMMENT '原字段：chr',
    `start` varchar(50) DEFAULT NULL COMMENT '原字段：start',
    `end` varchar(50) DEFAULT NULL COMMENT '原字段：end',
    `ref` varchar(1000) DEFAULT NULL COMMENT '原字段：ref',
    `alt` varchar(1000) DEFAULT NULL COMMENT '原字段：alt',
    `hom_het` varchar(50) DEFAULT NULL COMMENT '原字段：hom_het',
    `mut_depth` varchar(50) DEFAULT NULL COMMENT '原字段：mutDepth',
    `total_depth` varchar(50) DEFAULT NULL COMMENT '原字段：totalDepth',
    `mut_freq` varchar(50) DEFAULT NULL COMMENT '原字段：mutFreq',
    `func_known_gene` varchar(255) DEFAULT NULL COMMENT '原字段：Func_knownGene',
    `gene_known_gene` varchar(500) DEFAULT NULL COMMENT '原字段：Gene_knownGene',
    `exonic_func_known_gene` varchar(255) DEFAULT NULL COMMENT '原字段：ExonicFunc_knownGene',
    `aa_change_known_gene` text COMMENT '原字段：AAChange_knownGene',
    `esp6500si_all` varchar(255) DEFAULT NULL COMMENT '原字段：esp6500si_all',
    `c1000g2012apr_all` varchar(255) DEFAULT NULL COMMENT '原字段：1000g2012apr_all',
    `dbsnp_rs` varchar(500) DEFAULT NULL COMMENT '原字段：dbSNP_rs',
    `cosmic91` text COMMENT '原字段：cosmic91',
    `clinvar` text COMMENT '原字段：clinvar',
    `check_depth` varchar(50) DEFAULT NULL COMMENT '原字段：checkDepth',
    `check_result` varchar(255) DEFAULT NULL COMMENT '原字段：checkResult',
    `clinvar_result` text COMMENT 'Indel特有字段：clinvar_result',
    `sp_tag` varchar(255) DEFAULT NULL COMMENT 'Indel特有字段：Sp_Tag',
    `final_check` varchar(255) DEFAULT NULL COMMENT '原字段：Final_Check',
    `database_info` text COMMENT '原字段：Database_Info',
    `gene` varchar(255) DEFAULT NULL COMMENT '新增字段，逻辑同gene_known_gene',
    `transcript` varchar(255) DEFAULT NULL COMMENT '新增字段，转录本',
    `exon` varchar(255) DEFAULT NULL COMMENT '新增字段，外显子/内含子/启动子',
    `chgvs` varchar(255) DEFAULT NULL COMMENT '新增字段，氨基酸突变',
    `phgvs` varchar(255) DEFAULT NULL COMMENT '新增字段，蛋白质突变',
    `variant` varchar(255) DEFAULT NULL COMMENT '新增字段，知识库对应突变',
    `ori_variant` varchar(255) DEFAULT NULL COMMENT '新增字段，转录本 外显子 chgvs phgvs',
    `parent_mutation_id` bigint DEFAULT NULL COMMENT '人工关联的NKB父级mutation ID',
    `is_reported` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否纳入报告：0否，1是',
    `filtered_rationale` varchar(500) DEFAULT NULL COMMENT '过滤理由',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_somatic_snv_file` (`file_id`, `del_flag`),
    KEY `idx_file_somatic_parent_mutation` (`parent_mutation_id`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='SomaticSNV文件明细';

-- file_CNV
CREATE TABLE IF NOT EXISTS `file_CNV` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `gene` varchar(255) DEFAULT NULL COMMENT '原字段：gene',
    `chr` varchar(50) DEFAULT NULL COMMENT '原字段：chr',
    `start` varchar(50) DEFAULT NULL COMMENT '原字段：start',
    `end` varchar(50) DEFAULT NULL COMMENT '原字段：end',
    `copy_num` varchar(50) DEFAULT NULL COMMENT '原字段：copy_num',
    `variant` varchar(50) DEFAULT NULL COMMENT '新增字段：variant',
    `ori_variant` varchar(50) DEFAULT NULL COMMENT '原字段：ori_variant',
    `parent_mutation_id` bigint DEFAULT NULL COMMENT '人工关联的NKB父级mutation ID',
    `is_reported` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否纳入报告：0否，1是',
    `filtered_rationale` varchar(500) DEFAULT NULL COMMENT '过滤理由',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_cnv_file` (`file_id`, `del_flag`),
    KEY `idx_file_cnv_parent_mutation` (`parent_mutation_id`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='CNV文件明细';

-- file_Fusion
CREATE TABLE IF NOT EXISTS `file_Fusion` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `chromosome1` varchar(50) DEFAULT NULL COMMENT '原字段：chromosome1',
    `softclip1` varchar(50) DEFAULT NULL COMMENT '原字段：softclip1',
    `sclip1_info` text COMMENT '原字段：sclip1_info',
    `chromosome2` varchar(50) DEFAULT NULL COMMENT '原字段：chromosome2',
    `softclip2` varchar(50) DEFAULT NULL COMMENT '原字段：softclip2',
    `sclip2_info` text COMMENT '原字段：sclip2_info',
    `driver_gene` varchar(255) DEFAULT NULL COMMENT '原字段：driverGene',
    `cosmic_info` text COMMENT '原字段：cosmic_info',
    `db_info` text COMMENT '原字段：db_info',
    `stream` varchar(255) DEFAULT NULL COMMENT '原字段：stream',
    `sup_reads_hq` varchar(50) DEFAULT NULL COMMENT '原字段：sup_reads(hq)',
    `sup_reads_uniq` varchar(50) DEFAULT NULL COMMENT '原字段：sup_reads(uniq)',
    `depth` varchar(50) DEFAULT NULL COMMENT '原字段：depth',
    `freq` varchar(50) DEFAULT NULL COMMENT '原字段：freq',
    `check_result` varchar(255) DEFAULT NULL COMMENT '原字段：检测结果',
    `transcript` varchar(255) DEFAULT NULL COMMENT '原字段：转录本',
    `fusion_reads` varchar(255) DEFAULT NULL COMMENT '原字段：融合reads数',
    `sarcoma_subtypes` varchar(255) DEFAULT NULL COMMENT '原字段：基因变异相关肉瘤亚型',
    `evidence_level` varchar(255) DEFAULT NULL COMMENT '原字段：证据等级',
    `gene` varchar(255) DEFAULT NULL,
    `gene1` varchar(255) DEFAULT NULL,
    `bp1` varchar(255) DEFAULT NULL,
    `gene2` varchar(255) DEFAULT NULL,
    `bp2` varchar(255) DEFAULT NULL,
    `variant` varchar(255) DEFAULT NULL COMMENT 'gene1-gene2',
    `tag` varchar(255) DEFAULT NULL COMMENT 'RNA/DNA',
    `ori_variant` varchar(120) DEFAULT NULL,
    `parent_mutation_id` bigint DEFAULT NULL COMMENT '人工关联的NKB父级mutation ID',
    `is_reported` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否纳入报告：0否，1是',
    `filtered_rationale` varchar(500) DEFAULT NULL COMMENT '过滤理由',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_fusion_file` (`file_id`, `del_flag`),
    KEY `idx_file_fusion_parent_mutation` (`parent_mutation_id`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='Fusion文件明细';

-- file_CR_ALL
CREATE TABLE IF NOT EXISTS `file_CR_ALL` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `gene` varchar(255) DEFAULT NULL COMMENT '原字段：Gene',
    `chr` varchar(50) DEFAULT NULL COMMENT '原字段：Chr',
    `pos` varchar(50) DEFAULT NULL COMMENT '原字段：Pos',
    `transcript_id` varchar(255) DEFAULT NULL COMMENT '原字段：TranscriptID',
    `exon` varchar(100) DEFAULT NULL COMMENT '原字段：Exon',
    `chgvs` varchar(500) DEFAULT NULL COMMENT '原字段：cHGVS',
    `phgvs` varchar(500) DEFAULT NULL COMMENT '原字段：pHGVS',
    `zygosity` varchar(100) DEFAULT NULL COMMENT '原字段：Zygosity',
    `exonic_func` varchar(255) DEFAULT NULL COMMENT '原字段：ExonicFunc',
    `c1000g2015aug_all` varchar(100) DEFAULT NULL COMMENT '原字段：1000g2015aug_all',
    `exac_eas` varchar(100) DEFAULT NULL COMMENT '原字段：ExAC_EAS',
    `clinvar_id` varchar(255) DEFAULT NULL COMMENT '原字段：ClinvarID',
    `sift_pred` varchar(100) DEFAULT NULL COMMENT '原字段：SIFT_pred',
    `polyphen2_hdiv_pred` varchar(100) DEFAULT NULL COMMENT '原字段：Polyphen2_HDIV_pred',
    `mutation_taster_pred` varchar(100) DEFAULT NULL COMMENT '原字段：MutationTaster_pred',
    `depth` varchar(50) DEFAULT NULL COMMENT '原字段：depth',
    `revel` varchar(100) DEFAULT NULL COMMENT '原字段：REVEL',
    `gnomad_genome_all` varchar(100) DEFAULT NULL COMMENT '原字段：gnomAD_genome_ALL',
    `interpro_domain` text COMMENT '原字段：Interpro_domain',
    `clnsig` varchar(500) DEFAULT NULL COMMENT '原字段：CLNSIG',
    `omim_phenotypes` text COMMENT '原字段：OMIM_Phenotypes',
    `omim_id` varchar(500) DEFAULT NULL COMMENT '原字段：OMIM_ID',
    `hgmd_tag` varchar(255) DEFAULT NULL COMMENT '原字段：HGMD_tag',
    `hgmd_disease` text COMMENT '原字段：HGMD_disease',
    `hgmd_pmid` varchar(500) DEFAULT NULL COMMENT '原字段：HGMD_pmid',
    `classification_lovd` varchar(500) DEFAULT NULL COMMENT '原字段：classification_lovd',
    `clinical_significance_clinvar` varchar(500) DEFAULT NULL COMMENT '原字段：clinical_significance_clinvar',
    `source` varchar(255) DEFAULT NULL COMMENT '原字段：source',
    `clinical_significance_enigma` varchar(500) DEFAULT NULL COMMENT '原字段：clinical_significance_enigma',
    `variant` varchar(500) DEFAULT NULL COMMENT '新增字段：variant',
    `ori_variant` varchar(500) DEFAULT NULL COMMENT '新增字段：ori_variant',
    `parent_mutation_id` bigint DEFAULT NULL COMMENT '人工关联的NKB父级mutation ID',
    `is_reported` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否纳入报告：0否，1是',
    `filtered_rationale` varchar(500) DEFAULT NULL COMMENT '过滤理由',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_cr_all_file` (`file_id`, `del_flag`),
    KEY `idx_file_cr_parent_mutation` (`parent_mutation_id`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='CR_ALL文件明细';

-- file_MSI
CREATE TABLE IF NOT EXISTS `file_MSI` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `score` varchar(255) DEFAULT NULL COMMENT '原字段：Score',
    `threshold` varchar(255) DEFAULT NULL COMMENT '原字段：Threshold',
    `status` varchar(255) DEFAULT NULL COMMENT '原字段：Status',
    `check_result` varchar(255) DEFAULT NULL COMMENT '原字段：checkResult',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_msi_file` (`file_id`, `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='MSI文件明细';

-- file_qc
CREATE TABLE IF NOT EXISTS `file_qc` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `tumor_cell_content` varchar(255) DEFAULT NULL COMMENT '原字段：肿瘤細胞含量',
    `dna_total_ng` varchar(255) DEFAULT NULL COMMENT '原字段：DNA 总量（ng）',
    `dna_degradation` varchar(255) DEFAULT NULL COMMENT '原字段：DNA 降解程度',
    `pre_library_total_ng` varchar(255) DEFAULT NULL COMMENT '原字段：预文库总量（ng）',
    `sequencing_data_volume` varchar(255) DEFAULT NULL COMMENT '原字段：下机数据量',
    `mean_sequencing_depth` varchar(255) DEFAULT NULL COMMENT '原字段：平均测序深度',
    `coverage_uniformity` varchar(255) DEFAULT NULL COMMENT '原字段：覆盖均一性',
    `target_region_coverage` varchar(255) DEFAULT NULL COMMENT '原字段：目标区域覆盖度',
    `genome_alignment_rate` varchar(255) DEFAULT NULL COMMENT '原字段：基因组比对率',
    `base_quality_q30_rate` varchar(255) DEFAULT NULL COMMENT '原字段：碱基质量 Q30 占比',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_qc_file` (`file_id`, `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='qc文件明细';

-- file_qc_control
CREATE TABLE IF NOT EXISTS `file_qc_control` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `tumor_cell_content` varchar(255) DEFAULT NULL COMMENT '原字段：肿瘤細胞含量',
    `dna_total_ng` varchar(255) DEFAULT NULL COMMENT '原字段：DNA 总量（ng）',
    `dna_degradation` varchar(255) DEFAULT NULL COMMENT '原字段：DNA 降解程度',
    `pre_library_total_ng` varchar(255) DEFAULT NULL COMMENT '原字段：预文库总量（ng）',
    `sequencing_data_volume` varchar(255) DEFAULT NULL COMMENT '原字段：下机数据量',
    `mean_sequencing_depth` varchar(255) DEFAULT NULL COMMENT '原字段：平均测序深度',
    `coverage_uniformity` varchar(255) DEFAULT NULL COMMENT '原字段：覆盖均一性',
    `target_region_coverage` varchar(255) DEFAULT NULL COMMENT '原字段：目标区域覆盖度',
    `genome_alignment_rate` varchar(255) DEFAULT NULL COMMENT '原字段：基因组比对率',
    `base_quality_q30_rate` varchar(255) DEFAULT NULL COMMENT '原字段：碱基质量 Q30 占比',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_qc_file` (`file_id`, `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='对照样本qc文件明细';

-- file_chemical
CREATE TABLE IF NOT EXISTS `file_chemical` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `raw_json` longtext COMMENT '原始chemical.json完整内容',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_chemical_file` (`file_id`, `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='chemical.json文件明细';

-- history_somatic
CREATE TABLE IF NOT EXISTS `history_somatic` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `match_key` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化匹配条件SHA-256',
    `gene` varchar(100) NOT NULL,
    `variant` varchar(500) NOT NULL,
    `ori_variant` varchar(1000) NOT NULL,
    `disease_id` int DEFAULT NULL COMMENT 'NKB癌种ID',
    `disease` varchar(255) NOT NULL,
    `gender` varchar(16) NOT NULL,
    `customer` varchar(255) NOT NULL,
    `project_code` varchar(120) NOT NULL COMMENT 'analysis_data.product',
    `source_type` varchar(32) NOT NULL COMMENT 'SNP_INDEL/CNV/FUSION',
    `mutation_type` varchar(8) NOT NULL DEFAULT 'S',
    `measurement` varchar(100) DEFAULT NULL,
    `parent_mutation_id` bigint DEFAULT NULL COMMENT '人工改靶NKB mutation ID',
    `match_status` varchar(32) NOT NULL,
    `variation_class` varchar(32) DEFAULT NULL,
    `match_result` json NOT NULL COMMENT '冻结的ReportDrugPreview.Item JSON',
    `source_analysis_id` bigint NOT NULL,
    `source_report_id` bigint NOT NULL,
    `source_variant_id` bigint NOT NULL,
    `knowledge_matched_at` datetime(3) NOT NULL COMMENT '首次按知识库匹配时间',
    `create_time` datetime COMMENT '创建时间',
    `create_by`   bigint    COMMENT '创建者',
    `update_by`   bigint    COMMENT '更新者',
    `update_time` datetime  COMMENT '更新时间',
    `del_flag`    char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_history_somatic_match_key` (tenant_id, `match_key`),
    KEY `idx_history_somatic_project` (`customer`(64), `project_code`(64), `disease_id`, `gender`),
    KEY `idx_history_somatic_variant` (`gene`, `variant`(120), `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='体系位点药物匹配历史';

-- history_germline
CREATE TABLE IF NOT EXISTS `history_germline` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `match_key` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化匹配条件SHA-256',
    `gene` varchar(100) NOT NULL,
    `variant` varchar(500) NOT NULL,
    `ori_variant` varchar(1000) NOT NULL,
    `disease_id` int DEFAULT NULL COMMENT 'NKB癌种ID',
    `disease` varchar(255) NOT NULL,
    `gender` varchar(16) NOT NULL,
    `customer` varchar(255) NOT NULL,
    `project_code` varchar(120) NOT NULL COMMENT 'analysis_data.product',
    `source_type` varchar(32) NOT NULL DEFAULT 'CR_ALL',
    `mutation_type` varchar(8) NOT NULL DEFAULT 'G',
    `measurement` varchar(100) DEFAULT NULL COMMENT '胚系合子状态',
    `parent_mutation_id` bigint DEFAULT NULL COMMENT '人工改靶NKB mutation ID',
    `match_status` varchar(32) NOT NULL,
    `variation_class` varchar(32) DEFAULT NULL,
    `clinical_significance` tinyint NOT NULL DEFAULT 3 COMMENT '临床意义：1致病，2可能致病，3未知临床意义，4可能良性，5良性',
    `match_result` json NOT NULL COMMENT '冻结的ReportDrugPreview.Item JSON',
    `source_analysis_id` bigint NOT NULL,
    `source_report_id` bigint NOT NULL,
    `source_variant_id` bigint NOT NULL,
    `knowledge_matched_at` datetime(3) NOT NULL COMMENT '首次按知识库匹配时间',
    `create_time` datetime COMMENT '创建时间',
    `create_by`   bigint    COMMENT '创建者',
    `update_by`   bigint    COMMENT '更新者',
    `update_time` datetime  COMMENT '更新时间',
    `del_flag`    char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_history_germline_match_key` (tenant_id, `match_key`),
    KEY `idx_history_germline_project` (`customer`(64), `project_code`(64), `disease_id`, `gender`),
    KEY `idx_history_germline_variant` (`gene`, `variant`(120), `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='胚系位点药物匹配历史';

-- report_template
CREATE TABLE IF NOT EXISTS `report_template` (
    `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
    `template_code` VARCHAR(120) NOT NULL COMMENT '稳定模板编码',
    `template_name` VARCHAR(255) NOT NULL COMMENT '模板名称',
    `template_version` VARCHAR(40) NOT NULL DEFAULT 'v1' COMMENT '模板版本',
    `customer_code` VARCHAR(120) DEFAULT NULL COMMENT '客户编码',
    `report_type` VARCHAR(40) NOT NULL COMMENT '报告类型',
    `module_code` VARCHAR(500) NULL DEFAULT NULL COMMENT '有序个性化Java报告模块编码列表，分号分隔；公共模块无需配置',
    `template_path` VARCHAR(1000) NOT NULL COMMENT '项目内或受控模板路径',
    `template_sha256` CHAR(64) DEFAULT NULL COMMENT '模板文件SHA-256',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT 'ENABLED/DISABLED',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`template_id`),
    UNIQUE KEY `uk_report_template_code` (tenant_id, `template_code`),
    KEY `idx_report_template_module` (`module_code`, `status`, `del_flag`),
    KEY `idx_report_template_customer` (`customer_code`, `status`, `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='报告模板';

-- product_template
CREATE TABLE IF NOT EXISTS `product_template` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_id` bigint NOT NULL COMMENT '逻辑关联product_config.id（本仓产品主数据表）',
    `template_id` bigint NOT NULL COMMENT '逻辑关联report_template.template_id',
    `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认模板',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `sort_order` INT NOT NULL DEFAULT 0 COMMENT '展示顺序',
    `create_time` datetime COMMENT '创建时间',
    `create_by`   bigint    COMMENT '创建者',
    `update_by`   bigint    COMMENT '更新者',
    `update_time` datetime  COMMENT '更新时间',
    `del_flag`    char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`  varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_product_template_template` (`template_id`, `enabled`),
    KEY `idx_product_template_product` (`product_id`, `enabled`, `sort_order`),
    UNIQUE KEY uk_product_template (tenant_id, product_id, template_id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='产品与报告模板多对多';


-- file_Chem（参考工程库里有、但 report_management_schema.sql 未导出 → 从 172.20.1.34:8806/biotech 只读取 SHOW CREATE TABLE 后同样改造）
CREATE TABLE IF NOT EXISTS `file_Chem` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `file_id` bigint NOT NULL COMMENT '关联data_file_status.file_id',
    `chr` varchar(50) DEFAULT NULL COMMENT '原字段：chr',
    `position` varchar(50) DEFAULT NULL COMMENT '原字段：position',
    `allele1` varchar(255) DEFAULT NULL COMMENT '原字段：allele1',
    `reviewed_by` bigint DEFAULT NULL COMMENT '审核人ID',
    `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_file_chem_file` (`file_id`, `del_flag`),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='Chem文件明细';

-- ============================================================================
-- 菜单：「报告管理」（父，path='report'）›「报告解读」
-- 幂等：按 path 定位父菜单（不写死 menu_id）；NOT EXISTS 包派生表（MySQL 5.7 无 LATERAL）
-- ============================================================================
SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.report_interpretation', @report_menu_id, 2, 'interpretation', 'report/interpretation/index',
    1, 0, 'C', '0', '0', 'report:interpretation:list', 'local-icon-search', 1, NOW(), '报告解读'
FROM (SELECT 1) AS dummy
WHERE @report_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @report_menu_id AND path = 'interpretation') AS c0);

SET @menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @report_menu_id AND path = 'interpretation' AND menu_type = 'C' LIMIT 1);

-- 按钮权限（F）：与 @SaCheckPermission 逐字一致，不进侧边栏
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT t.menu_name, @menu_id, t.order_num, '', NULL, 1, 0, 'F', '0', '0', t.perms, '#', 1, NOW(), t.remark
FROM (SELECT '查询' AS menu_name, 1 AS order_num, 'report:interpretation:query' AS perms, '查询解读列表' AS remark
      UNION ALL SELECT '进入解读', 2, 'report:interpretation:enter', '创建/复用报告记录'
      UNION ALL SELECT '位点编辑', 3, 'report:interpretation:edit', '入报告开关、改靶、临床意义'
      UNION ALL SELECT '报告预览', 4, 'report:interpretation:preview', '模板数据预览'
      UNION ALL SELECT '报告审核', 5, 'report:interpretation:review', '提交审核/通过/驳回'
      UNION ALL SELECT '报告发送', 6, 'report:interpretation:send', '发送报告邮件') AS t
WHERE @menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = @menu_id AND x.perms = t.perms);

-- 授权给超级管理员角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @report_menu_id OR m.menu_id = @menu_id OR m.parent_id = @menu_id;


-- ============================================================================
-- 种子：报告模板 + 产品模板关系（Tab④ 报告预览按 template_code 取模板）
-- 本地 product_config 现只有一条：id=2 / code=BTP001 / novopm2_tis_1238_shengyu
-- ============================================================================
INSERT INTO report_template (template_code, template_name, template_version, customer_code, report_type,
    module_code, template_path, status, create_by, create_time, tenant_id)
SELECT 'pharma-shengyu', '圣域 1238 报告', 'v1', NULL, 'SOMATIC',
    NULL, 'report-templates/pharma-shengyu/v1/template.docx', 'ENABLED', 1, NOW(), '000000'
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM (SELECT template_id FROM report_template
    WHERE template_code = 'pharma-shengyu') AS c1);

INSERT INTO product_template (product_id, template_id, is_default, enabled, sort_order, create_time, tenant_id)
SELECT pc.id, rt.template_id, 1, 1, 0, NOW(), '000000'
FROM product_config pc, report_template rt
WHERE pc.code = 'BTP001' AND rt.template_code = 'pharma-shengyu'
  -- 相关子查询直接写在 NOT EXISTS 里：MySQL 5.7 的派生表不能引用外层别名（包一层 (SELECT ...) 会报 Unknown column 'pc.id'）
  AND NOT EXISTS (SELECT 1 FROM product_template pt
      WHERE pt.product_id = pc.id AND pt.template_id = rt.template_id);
