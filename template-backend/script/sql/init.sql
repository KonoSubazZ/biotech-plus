-- 数据库初始化脚本（由模板 base + business/*.sql 生成）
-- 基础表（sys_* / 租户 / 权限 / 字典等）：结构 + 数据
-- 业务表（样本 / 质控 / 报告 / 项目 / 合规 / 备份等）：仅结构，无业务数据
-- 重新生成：bash tools/build_init_sql.sh（或见仓库说明）

SET NAMES utf8mb4;

-- MySQL dump 10.13  Distrib 5.7.26, for Linux (x86_64)
--
-- Host: localhost    Database: bp_init_build
-- ------------------------------------------------------
-- Server version	5.7.26-log

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `analysis_data`
--

DROP TABLE IF EXISTS `analysis_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `analysis_data` (
  `analysis_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '分析数据ID',
  `analysis_batch_date` varchar(14) NOT NULL COMMENT '上传批次目录名 YYYYMMDDHHMMSS（批次身份）',
  `analysis_date` varchar(8) NOT NULL COMMENT '分析日期YYYYMMDD',
  `subbarcode` varchar(80) NOT NULL COMMENT '样本编号',
  `product_bio` varchar(120) NOT NULL COMMENT '生信产品编码（= 上传目录第 2 层 = product_config.product_bio）',
  `product_id` bigint(20) DEFAULT NULL COMMENT '逻辑关联product_config.id（本仓产品主数据表）',
  `drive_status` int(11) DEFAULT NULL COMMENT '驱动状态（10 驱动失败 / 11 驱动成功 / 12 驱动中）',
  `drive_executed_by` varchar(80) DEFAULT NULL COMMENT '驱动执行人',
  `drive_executed_at` datetime DEFAULT NULL COMMENT '驱动执行时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`analysis_id`),
  KEY `idx_analysis_product_id` (`product_id`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_analysis_batch` (`analysis_batch_date`,`product_bio`,`subbarcode`),
  KEY `idx_analysis_date_product` (`analysis_date`,`product_bio`),
  KEY `idx_analysis_subbarcode` (`subbarcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='样本分析数据';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `analysis_data`
--

/*!40000 ALTER TABLE `analysis_data` DISABLE KEYS */;
/*!40000 ALTER TABLE `analysis_data` ENABLE KEYS */;

--
-- Table structure for table `analysis_file_status`
--

DROP TABLE IF EXISTS `analysis_file_status`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `analysis_file_status` (
  `file_id` bigint(20) NOT NULL AUTO_INCREMENT,
  `file_path` varchar(512) NOT NULL COMMENT '文件绝对路径',
  `file_name` varchar(120) NOT NULL,
  `data_type` varchar(40) NOT NULL,
  `file_type` varchar(40) NOT NULL,
  `file_text` longtext,
  `file_size` bigint(20) DEFAULT NULL COMMENT '文件字节数（0 = 空文件）',
  `analysis_date` varchar(8) NOT NULL,
  `subbarcode` varchar(80) NOT NULL,
  `product_bio` varchar(120) NOT NULL COMMENT '生信产品编码（冗余展示，= analysis_data.product_bio）',
  `status` varchar(20) NOT NULL,
  `message` varchar(2000) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `mut_num` int(11) DEFAULT NULL,
  `analysis_id` bigint(20) NOT NULL,
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`file_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_file_analysis` (`analysis_id`,`file_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='样本文件驱动状态';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `analysis_file_status`
--

/*!40000 ALTER TABLE `analysis_file_status` DISABLE KEYS */;
/*!40000 ALTER TABLE `analysis_file_status` ENABLE KEYS */;

--
-- Table structure for table `analysis_report`
--

DROP TABLE IF EXISTS `analysis_report`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `analysis_report` (
  `report_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '报告ID',
  `analysis_id` bigint(20) NOT NULL,
  `analysis_batch_date` varchar(14) NOT NULL COMMENT '上传批次目录名快照（= analysis_data.analysis_batch_date）',
  `analysis_date` varchar(8) NOT NULL,
  `subbarcode` varchar(80) NOT NULL COMMENT '样本编号',
  `product_bio` varchar(120) NOT NULL COMMENT '生信产品编码快照（= analysis_data.product_bio）',
  `product_id` bigint(20) DEFAULT NULL COMMENT '产品ID',
  `project_no` varchar(60) NOT NULL COMMENT '项目编号快照（= cms_sample.project_no）',
  `analysis_disease_id` bigint(20) DEFAULT NULL COMMENT '解读癌种ID',
  `analysis_disease` varchar(255) DEFAULT NULL,
  `cancer_type` varchar(256) DEFAULT NULL COMMENT 'LIMS癌种（myapp_webcrmsample.CANCERTYPE）',
  `chem_disease` varchar(255) DEFAULT NULL COMMENT '化疗癌种',
  `report_type` varchar(40) DEFAULT NULL COMMENT '报告类型',
  `report_file_raw_path` varchar(1000) DEFAULT NULL COMMENT '原始DOCX报告路径',
  `report_file_path` varchar(1000) DEFAULT NULL COMMENT '报告路径',
  `sub_report_file_path` varchar(255) DEFAULT NULL COMMENT '子报告路径',
  `template` varchar(255) DEFAULT NULL COMMENT '报告模板',
  `report_generated_by` bigint(20) DEFAULT NULL COMMENT '报告生成人（sys_user.user_id）',
  `report_generated_at` datetime DEFAULT NULL COMMENT '报告生成时间',
  `report_checked_by` bigint(20) DEFAULT NULL COMMENT '报告复核人（sys_user.user_id）',
  `report_checked_at` datetime DEFAULT NULL COMMENT '报告审核时间',
  `report_sent_by` bigint(20) DEFAULT NULL COMMENT '报告发送人（sys_user.user_id）',
  `report_sent_at` datetime DEFAULT NULL COMMENT '报告发放时间',
  `comment` varchar(300) DEFAULT NULL COMMENT '备注',
  `report_status` int(11) NOT NULL DEFAULT '20' COMMENT '报告状态（20 生信看点 / 21 生信审核 / 30 报告待解读 / 31 报告解读中 / 32 报告生成成功 / 33 报告生成失败 / 40 报告待审核 / 41 报告审核中 / 42 报告审核失败 / 43 报告审核成功 / 50 报告签名中 / 52 报告签名失败 / 53 报告签名成功 / 60 报告待发送 / 61 报告发送失败 / 62 报告发送成功）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `template_id` bigint(20) DEFAULT NULL COMMENT '逻辑关联report_template.template_id',
  `report_json_path` varchar(1000) DEFAULT NULL COMMENT '正式生成JSON文件路径',
  `report_name` varchar(500) DEFAULT NULL COMMENT '最近一次生成的报告名（命名模板渲染结果，不含唯一后缀）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  `report_signed_file_path` varchar(1000) DEFAULT NULL COMMENT '签名后PDF报告路径（上上签回传，最终交付）',
  `report_sign_started_at` datetime DEFAULT NULL COMMENT '签名开始时间（送签时间）',
  `report_sign_flow_no` varchar(80) DEFAULT NULL COMMENT '上上签合同/流程号',
  `report_sign_time` datetime DEFAULT NULL COMMENT '签名完成时间（上上签回调）',
  `report_bioinfo_viewed_by` bigint(20) DEFAULT NULL COMMENT '生信看点人（sys_user.user_id）',
  `report_bioinfo_viewed_at` datetime DEFAULT NULL COMMENT '生信看点完成时间',
  `report_bioinfo_reviewed_by` bigint(20) DEFAULT NULL COMMENT '生信复核人（sys_user.user_id）',
  `report_bioinfo_reviewed_at` datetime DEFAULT NULL COMMENT '生信复核时间',
  `report_interpreted_by` bigint(20) DEFAULT NULL COMMENT '报告解读人（认领人 sys_user.user_id）',
  `report_interpreted_at` datetime DEFAULT NULL COMMENT '报告解读认领时间',
  `report_review_claimed_by` bigint(20) DEFAULT NULL COMMENT '报告复核认领人（sys_user.user_id）',
  `report_review_claimed_at` datetime DEFAULT NULL COMMENT '报告复核认领时间',
  `report_pathologist_by` bigint(20) DEFAULT NULL COMMENT '病理师（固定账号 bingli，sys_user.user_id）',
  `report_revert_from` tinyint(4) DEFAULT NULL COMMENT '回退来源状态（审核失败回退时记原状态）',
  `report_revert_reason` varchar(300) DEFAULT NULL COMMENT '回退原因（审核失败）',
  `report_gen_fail_reason` varchar(500) DEFAULT NULL COMMENT '报告生成失败原因（33）',
  `report_sign_fail_reason` varchar(300) DEFAULT NULL COMMENT '报告签名失败原因（52）',
  `report_version` int(11) DEFAULT NULL COMMENT '业务版本，首次生成时预留',
  `report_sequence_id` bigint(20) DEFAULT NULL COMMENT '业务版本序列ID',
  `report_generation_no` int(11) NOT NULL DEFAULT '0' COMMENT '内部生成轮次',
  `report_reviewer_id` bigint(20) DEFAULT NULL COMMENT '重生成后的定向复审人',
  `source_report_id` bigint(20) DEFAULT NULL COMMENT '重新解读来源报告ID',
  `report_variant_edits` json DEFAULT NULL COMMENT '本次报告的人工修改：{"BRCA1_A2825E_CR_ALL:24":{"parentMutationIds":[1,2],"clinicalSignificance":1,"mutationDescription":"..."}}；键=gene_variant_sourceType:sourceId；NULL=无修改',
  PRIMARY KEY (`report_id`),
  KEY `idx_report_analysis` (`analysis_id`,`del_flag`),
  KEY `idx_report_status` (`report_status`),
  KEY `idx_report_template_id` (`template_id`,`report_status`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_report_sign` (`report_sign_flow_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='分析报告';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `analysis_report`
--

/*!40000 ALTER TABLE `analysis_report` DISABLE KEYS */;
/*!40000 ALTER TABLE `analysis_report` ENABLE KEYS */;

--
-- Table structure for table `analysis_report_email_log`
--

DROP TABLE IF EXISTS `analysis_report_email_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `analysis_report_email_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `trace_id` char(32) NOT NULL COMMENT '邮件发送追踪号',
  `analysis_id` bigint(20) NOT NULL COMMENT '分析数据ID',
  `report_id` bigint(20) NOT NULL COMMENT '报告ID',
  `recipients` varchar(2000) NOT NULL COMMENT '收件人，分号分隔',
  `recipient_count` bigint(20) NOT NULL COMMENT '收件人数',
  `template_code` varchar(80) DEFAULT NULL COMMENT '邮件模板标识',
  `status` varchar(20) NOT NULL COMMENT 'SENDING/SUCCESS/FAILED',
  `provider_message_id` varchar(255) DEFAULT NULL COMMENT '邮件服务商消息ID',
  `failure_reason` varchar(1000) DEFAULT NULL COMMENT '失败原因摘要',
  `triggered_by` varchar(80) NOT NULL COMMENT '触发人',
  `requested_at` datetime NOT NULL COMMENT '发起时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_email_trace` (`tenant_id`,`trace_id`),
  KEY `idx_report_email_report` (`report_id`,`requested_at`),
  KEY `idx_report_email_analysis` (`analysis_id`,`requested_at`),
  KEY `idx_report_email_status` (`status`,`requested_at`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='报告邮件发送审计日志';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `analysis_report_email_log`
--

/*!40000 ALTER TABLE `analysis_report_email_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `analysis_report_email_log` ENABLE KEYS */;

--
-- Table structure for table `backup_db_binlog`
--

DROP TABLE IF EXISTS `backup_db_binlog`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `backup_db_binlog` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance` varchar(64) NOT NULL DEFAULT 'biotech-plus-mysql' COMMENT '实例标识',
  `backup_date` date NOT NULL COMMENT '归档日期',
  `db_scope` varchar(200) NOT NULL DEFAULT 'biotech,nkb' COMMENT '本包覆盖的库（恢复时 --database 过滤回放）',
  `start_file` varchar(64) NOT NULL COMMENT '起点 binlog 文件名（= 上一条 end 或全量基线）',
  `start_pos` bigint(20) NOT NULL COMMENT '起点位点',
  `end_file` varchar(64) NOT NULL COMMENT '终点 binlog 文件名（备份瞬间 SHOW MASTER STATUS）',
  `end_pos` bigint(20) NOT NULL COMMENT '终点位点',
  `file_count` int(11) NOT NULL DEFAULT '0' COMMENT 'binlog 文件个数',
  `total_size` bigint(20) NOT NULL DEFAULT '0' COMMENT '本包总字节',
  `files_json` text COMMENT '文件清单 JSON：[{file,size,md5}]',
  `local_dir` varchar(1024) DEFAULT NULL COMMENT '本地目录',
  `local_pruned` tinyint(4) NOT NULL DEFAULT '0' COMMENT '本地是否已清理（0 在 / 1 已清）',
  `oss_prefix` varchar(1024) DEFAULT NULL COMMENT 'OSS 前缀',
  `duration_ms` bigint(20) DEFAULT NULL COMMENT '耗时（毫秒）',
  `config_hash` char(40) DEFAULT NULL COMMENT '执行时 backup.yml 的 sha1',
  `script_version` varchar(32) DEFAULT NULL COMMENT '脚本 git 短 commit',
  `backup_status` tinyint(4) NOT NULL DEFAULT '2' COMMENT '预留状态：0 待 / 1 中 / 2 成功（本期只写 2）',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者（脚本上报为 sys_backup）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bdb_inst_date` (`instance`,`backup_date`),
  KEY `idx_bdb_date` (`backup_date`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='binlog 增量归档记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backup_db_binlog`
--

/*!40000 ALTER TABLE `backup_db_binlog` DISABLE KEYS */;
/*!40000 ALTER TABLE `backup_db_binlog` ENABLE KEYS */;

--
-- Table structure for table `backup_db_full`
--

DROP TABLE IF EXISTS `backup_db_full`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `backup_db_full` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `instance` varchar(64) NOT NULL DEFAULT 'biotech-plus-mysql' COMMENT '实例标识',
  `db_scope` varchar(200) NOT NULL COMMENT '本包覆盖的库（逗号分隔；绑定备份=一组库一个包）',
  `backup_date` date NOT NULL COMMENT '备份日期',
  `backup_type` varchar(16) NOT NULL DEFAULT 'weekly' COMMENT '类型：weekly 周定 / manual 手工补跑',
  `excluded_tables` varchar(1000) NOT NULL DEFAULT '' COMMENT '本份 dump 排除的表（脚本按配置展开后的实际表名，逗号分隔）',
  `table_count` int(11) DEFAULT NULL COMMENT 'dump 内 CREATE TABLE 数（已扣除排除表）',
  `db_table_total` int(11) DEFAULT NULL COMMENT '该库实际表数（对账：table_count + 排除数）',
  `binlog_file` varchar(64) DEFAULT NULL COMMENT '基线 binlog 文件（mysqldump --master-data=2 记录）',
  `binlog_pos` bigint(20) DEFAULT NULL COMMENT '基线位点（PITR 起点）',
  `local_path` varchar(1024) DEFAULT NULL COMMENT '本地压缩包路径',
  `local_md5` varchar(32) DEFAULT NULL COMMENT '本地压缩包 MD5',
  `local_size` bigint(20) DEFAULT NULL COMMENT '压缩包字节数',
  `local_pruned` tinyint(4) NOT NULL DEFAULT '0' COMMENT '本地是否已按保留策略清理（0 在 / 1 已清，OSS 仍在）',
  `oss_path` varchar(1024) DEFAULT NULL COMMENT 'OSS 对象路径（oss.enabled=false 时为空）',
  `oss_meta_md5` varchar(32) DEFAULT NULL COMMENT 'OSS x-oss-meta-file-md5 回读值',
  `duration_ms` bigint(20) DEFAULT NULL COMMENT '全流程耗时（毫秒）',
  `config_hash` char(40) DEFAULT NULL COMMENT '执行时 backup.yml 的 sha1（溯源用）',
  `script_version` varchar(32) DEFAULT NULL COMMENT '脚本 git 短 commit（溯源用）',
  `backup_status` tinyint(4) NOT NULL DEFAULT '2' COMMENT '预留状态：0 待 / 1 中 / 2 成功（本期只写 2）',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `finished_at` datetime DEFAULT NULL COMMENT '结束时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者（脚本上报为 sys_backup）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bdf_inst_date_type` (`instance`,`backup_date`,`backup_type`),
  KEY `idx_bdf_scope` (`db_scope`),
  KEY `idx_bdf_date` (`backup_date`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='数据库全量备份记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backup_db_full`
--

/*!40000 ALTER TABLE `backup_db_full` DISABLE KEYS */;
/*!40000 ALTER TABLE `backup_db_full` ENABLE KEYS */;

--
-- Table structure for table `backup_sample_file`
--

DROP TABLE IF EXISTS `backup_sample_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `backup_sample_file` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sample_id` bigint(20) NOT NULL COMMENT 'cms_sample.id',
  `report_id` bigint(20) DEFAULT NULL COMMENT 'analysis_report.report_id（一样本多报告时用于归属）',
  `subbarcode` varchar(100) NOT NULL COMMENT '样本编号',
  `project_no` varchar(100) DEFAULT NULL COMMENT '项目编号（cms_sample.project_no）',
  `order_code` varchar(100) DEFAULT NULL COMMENT '订单批次编号（cms_sample.order_code）',
  `product_bio` varchar(120) DEFAULT NULL COMMENT '生信产品编码（analysis_data.product_bio）',
  `research_center` varchar(200) DEFAULT NULL COMMENT '研究中心/客户名称',
  `file_category` varchar(32) NOT NULL COMMENT 'report_docx/report_pdf/report_json/report_signed_pdf/notify_*/fastq/bam/vcf/other',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名（含扩展名）',
  `server_host` varchar(64) NOT NULL DEFAULT '' COMMENT '文件所在主机（对应 sample.hosts 的键）',
  `server_path` varchar(1024) NOT NULL COMMENT '宿主绝对路径（脚本直接读）',
  `path_hash` char(32) NOT NULL COMMENT 'md5(host:path)，幂等键',
  `file_size` bigint(20) DEFAULT NULL COMMENT '字节数',
  `file_md5` varchar(32) DEFAULT NULL COMMENT '源文件 MD5',
  `oss_path` varchar(1024) DEFAULT NULL COMMENT 'OSS 对象 key',
  `oss_meta_md5` varchar(32) DEFAULT NULL COMMENT 'OSS x-oss-meta-file-md5 回读值',
  `backup_status` tinyint(4) NOT NULL DEFAULT '0' COMMENT '0待备份/1备份中/2成功/3失败/4跳过',
  `backup_time` datetime DEFAULT NULL COMMENT '备份完成时间',
  `fail_reason` varchar(500) DEFAULT NULL COMMENT '失败原因',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bsf_sample_path` (`sample_id`,`path_hash`),
  KEY `idx_bsf_report` (`report_id`),
  KEY `idx_bsf_status` (`sample_id`,`backup_status`),
  KEY `idx_bsf_subbarcode` (`subbarcode`,`file_category`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='样本备份文件明细（一物理文件一行）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `backup_sample_file`
--

/*!40000 ALTER TABLE `backup_sample_file` DISABLE KEYS */;
/*!40000 ALTER TABLE `backup_sample_file` ENABLE KEYS */;

--
-- Table structure for table `bioinfo_qc`
--

DROP TABLE IF EXISTS `bioinfo_qc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `bioinfo_qc` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subbarcode` varchar(100) NOT NULL COMMENT '样本编号（= cms_sample.subbarcode，源 sample_qc_info.sample_id）',
  `source_id` varchar(64) DEFAULT NULL COMMENT '上游记录 id（源 sample_qc_info.id，幂等/追溯用）',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '质控状态（passed 合格 / failed 不合格 / pending 待判定）',
  `notify_status` varchar(20) DEFAULT NULL COMMENT '通知发送状态（已生成（文档）/ 已发送（邮件）），暂不展示',
  `bam` varchar(255) DEFAULT NULL COMMENT 'bam',
  `gender` varchar(10) DEFAULT NULL COMMENT 'gender',
  `lib_id` varchar(255) DEFAULT NULL COMMENT 'lib_id',
  `paired` varchar(20) DEFAULT NULL COMMENT 'paired',
  `raw_gc` decimal(10,2) DEFAULT NULL COMMENT 'raw_gc',
  `hot_pos` varchar(100) DEFAULT NULL COMMENT 'hot_pos',
  `raw_q20` decimal(10,2) DEFAULT NULL COMMENT 'raw_q20',
  `raw_q30` decimal(10,2) DEFAULT NULL COMMENT 'raw_q30',
  `pipeline` varchar(50) DEFAULT NULL COMMENT 'pipeline',
  `raw_base` decimal(10,2) DEFAULT NULL COMMENT 'raw_base',
  `ssh_path` varchar(500) DEFAULT NULL COMMENT 'ssh_path',
  `duplicate` decimal(10,2) DEFAULT NULL COMMENT 'duplicate',
  `peak_size` decimal(10,2) DEFAULT NULL COMMENT 'peak_size',
  `qc_result` varchar(100) DEFAULT NULL COMMENT 'qc_result',
  `sqi16` varchar(50) DEFAULT NULL COMMENT 'sqi16',
  `bio_result` varchar(50) DEFAULT NULL COMMENT 'bio_result',
  `peak_range` varchar(50) DEFAULT NULL COMMENT 'peak_range',
  `concordance` varchar(50) DEFAULT NULL COMMENT 'concordance',
  `chrall` json DEFAULT NULL COMMENT 'chrall',
  `library_size` decimal(12,2) DEFAULT NULL COMMENT 'library_size',
  `bam_realign` varchar(255) DEFAULT NULL COMMENT 'bam_realign',
  `target_size` bigint(20) DEFAULT NULL COMMENT 'target_size',
  `total_reads` bigint(20) DEFAULT NULL COMMENT 'total_reads',
  `mapping_rate` decimal(10,2) DEFAULT NULL COMMENT 'mapping_rate',
  `average_depth` decimal(12,2) DEFAULT NULL COMMENT 'average_depth',
  `coverage_target` decimal(10,2) DEFAULT NULL COMMENT 'coverage_target',
  `mapped_reads` bigint(20) DEFAULT NULL COMMENT 'mapped_reads',
  `duplicate_target` decimal(10,2) DEFAULT NULL COMMENT 'duplicate_target',
  `contamination_level` decimal(10,4) DEFAULT NULL COMMENT 'contamination_level（源值带 %，入库去掉）',
  `coverage_target_50x` decimal(10,2) DEFAULT NULL COMMENT 'coverage_target_50x',
  `probe_capture_ratio` decimal(10,2) DEFAULT NULL COMMENT 'probe_capture_ratio',
  `coverage_target_500x` decimal(10,2) DEFAULT NULL COMMENT 'coverage_target_500x',
  `coverage_target_1000x` decimal(10,2) DEFAULT NULL COMMENT 'coverage_target_1000x',
  `coverage_uniformity_02xad_on_target` decimal(10,2) DEFAULT NULL COMMENT 'coverage_uniformity_02xad_on_target',
  `before_duplicate` bigint(20) DEFAULT NULL COMMENT 'before_duplicate',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bioinfo_qc_subbarcode` (`tenant_id`,`subbarcode`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_bioinfo_qc_lib_id` (`tenant_id`,`lib_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生信质控';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `bioinfo_qc`
--

/*!40000 ALTER TABLE `bioinfo_qc` DISABLE KEYS */;
/*!40000 ALTER TABLE `bioinfo_qc` ENABLE KEYS */;

--
-- Table structure for table `cms_sample`
--

DROP TABLE IF EXISTS `cms_sample`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_sample` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_no` varchar(128) DEFAULT NULL COMMENT '订单批次编号；接口字段 abatchno',
  `customer_name` varchar(100) DEFAULT NULL COMMENT '客户名称；接口字段 corpdesc',
  `contract_name` varchar(100) DEFAULT NULL COMMENT '合同名称；接口字段 contractname',
  `contract_no` varchar(128) DEFAULT NULL COMMENT '合同编号；接口字段 contractsno（设计列 CONTRACTCODE）',
  `scheme_no` varchar(128) DEFAULT NULL COMMENT '研究方案编号；接口字段 schemeno',
  `project_no` varchar(128) DEFAULT NULL COMMENT '项目编号；接口字段 yqprojectno',
  `contract_contact_email` varchar(100) DEFAULT NULL COMMENT '联系人邮箱（合同-甲方）；接口字段 contractemail',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '运营经理；接口字段 operatemanagerdesc',
  `research_center` varchar(100) DEFAULT NULL COMMENT '研究中心名称；接口字段 researchcenter',
  `report_receiver` varchar(100) DEFAULT NULL COMMENT '报告接收人；接口字段 REPORTRECEIVER',
  `report_email` varchar(100) DEFAULT NULL COMMENT '报告接收邮箱；接口字段 reportemail',
  `sender_name` varchar(100) DEFAULT NULL COMMENT '送检人；接口字段 sendpeople',
  `sender_phone` varchar(100) DEFAULT NULL COMMENT '联系电话；接口字段 sendertel',
  `send_date` varchar(10) DEFAULT NULL COMMENT '送检日期；接口字段 SENDDATE',
  `express_company` varchar(100) DEFAULT NULL COMMENT '快递公司；接口字段 expressname（设计列 DESC6）',
  `express_no` varchar(100) DEFAULT NULL COMMENT '快递单号；接口字段 expressno（设计列 DESC7）',
  `transport_condition` varchar(100) DEFAULT NULL COMMENT '快递条件；接口字段 transportcondition（设计列 DESC8）',
  `has_temperature_logger` varchar(16) DEFAULT NULL COMMENT '是否带温控计；接口字段 hascontroller',
  `receive_confirm_time` varchar(19) DEFAULT NULL COMMENT '收样确认时间；接口字段 receivetime（设计列 AUDITTIME）',
  `order_submit_time` varchar(19) DEFAULT NULL COMMENT '提交时间；接口字段 submittime',
  `order_no` varchar(128) DEFAULT NULL COMMENT '订单编号；接口字段 ORDERCODE（设计列 DESC50）',
  `subbarcode` varchar(128) NOT NULL COMMENT '诺禾编号；接口字段 BARCODE（设计列 DESC3）',
  `customer_sampling_code` varchar(100) DEFAULT NULL COMMENT '客户采样识别码；接口字段 customersamplingcode（设计列 DESC24）',
  `sample_name` varchar(100) DEFAULT NULL COMMENT '样本名称；接口字段 samplename（设计列 DESC23）',
  `subject_no` varchar(100) DEFAULT NULL COMMENT '试验参与者编号；接口字段 subjectid（设计列 CUSTOMEREXPNUM）',
  `sex` varchar(16) DEFAULT NULL COMMENT '性别；接口字段 SEX（设计列 DESC12）',
  `birth_year` char(4) DEFAULT NULL COMMENT '出生年份；接口字段 birthyear（设计列 BIRTH）',
  `disease_type` varchar(100) DEFAULT NULL COMMENT '临床诊断；接口字段 diseasetype（设计列 desc17）',
  `visit_cycle` varchar(100) DEFAULT NULL COMMENT '访视周期；接口字段 visitcycle（设计列 desc30）',
  `product_code` varchar(100) DEFAULT NULL COMMENT '产品编码；接口字段 PCODE（设计列 DESC25）',
  `product_name` varchar(100) DEFAULT NULL COMMENT '产品名称；接口字段 ERPTESTNAME（设计列 DESC2）',
  `sample_type` varchar(100) DEFAULT NULL COMMENT '样本类型；接口字段 SAMPLETYPE（设计列 DESC1）',
  `sample_quantity` varchar(32) DEFAULT NULL COMMENT '样本数量；接口字段 SPECIMENNUM（设计列 DESC4）',
  `quantity_unit` varchar(32) DEFAULT NULL COMMENT '数量单位；接口字段 UNIT（设计列 DESC11）',
  `sample_attribute` varchar(100) DEFAULT NULL COMMENT '样本属性；接口字段 SAMPLEATTRIBUTE（设计列 DESC67）',
  `collection_site` varchar(100) DEFAULT NULL COMMENT '取材部位；接口字段 collectionsite（设计列 desc16）',
  `tissue_type` varchar(100) DEFAULT NULL COMMENT '组织类型；接口字段 tissuetype（设计列 desc18）',
  `tumor_type` varchar(100) DEFAULT NULL COMMENT '肿瘤类型；接口字段 tumortype（设计列 desc19）',
  `primary_tumor_site` varchar(100) DEFAULT NULL COMMENT '原发部位；接口字段 primarytumorsite（设计列 desc20）',
  `collection_date` varchar(10) DEFAULT NULL COMMENT '采集日期；接口字段 SAMPLETIME（设计列 DESC34）',
  `assay_method` varchar(100) DEFAULT NULL COMMENT '检测方式；接口字段 assaymethod（设计列 desc21）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注；接口字段 SAMPLEREMARK（设计列 REMARK）',
  `abnormal_remark` text COMMENT '异常备注；接口字段 ABNORMALREMARK（设计列 DESC100）',
  `control_type` varchar(100) DEFAULT NULL COMMENT '管控类型；接口字段 mtacontroltype',
  `arrival_time` varchar(19) DEFAULT NULL COMMENT '到达时间；接口字段 arrivaltime（设计列 DYDATE）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `sync_log` text COMMENT '最近一次同步/刷新的变更明细（JSON 数组：field/label/oldValue/newValue/by/at）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
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
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cms_sample`
--

/*!40000 ALTER TABLE `cms_sample` DISABLE KEYS */;
/*!40000 ALTER TABLE `cms_sample` ENABLE KEYS */;

--
-- Table structure for table `cms_sample_change_log`
--

DROP TABLE IF EXISTS `cms_sample_change_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `cms_sample_change_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subbarcode` varchar(128) NOT NULL COMMENT '诺禾样本编号（刷新会重建行、id 会变，所以按编号关联）',
  `source` varchar(16) NOT NULL COMMENT '来源（refresh=行内从CMS更新 / edit=页面编辑）',
  `field` varchar(64) NOT NULL COMMENT '字段属性名，如 sampleType',
  `label` varchar(64) DEFAULT NULL COMMENT '字段中文名，如 样本类型',
  `old_value` text COMMENT '旧值',
  `new_value` text COMMENT '新值',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人（登录名）',
  `operate_time` datetime NOT NULL COMMENT '操作时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_sample_time` (`tenant_id`,`subbarcode`,`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='样本信息变更流水（历史修改信息；追加写，不覆盖）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cms_sample_change_log`
--

/*!40000 ALTER TABLE `cms_sample_change_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `cms_sample_change_log` ENABLE KEYS */;

--
-- Table structure for table `compliance_document`
--

DROP TABLE IF EXISTS `compliance_document`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `compliance_document` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `doc_type` varchar(20) NOT NULL COMMENT '文档类型（IQ 安装确认 / OQ 运行确认 / PQ 性能确认 / DEV_TEST 开发测试）',
  `title` varchar(200) NOT NULL COMMENT '文档标题',
  `version` varchar(50) NOT NULL COMMENT '版本号',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文档文件名（原文件名，附件存服务器固定目录）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_compliance_doc_type` (`tenant_id`,`doc_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='3Q 文档管理';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `compliance_document`
--

/*!40000 ALTER TABLE `compliance_document` DISABLE KEYS */;
/*!40000 ALTER TABLE `compliance_document` ENABLE KEYS */;

--
-- Table structure for table `delivery_email_log`
--

DROP TABLE IF EXISTS `delivery_email_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `delivery_email_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` char(32) NOT NULL COMMENT '邮件发送追踪号（UUID 去横线）',
  `target_type` varchar(16) NOT NULL COMMENT '交付物类型：report / notify',
  `target_id` bigint(20) NOT NULL COMMENT '报告ID / 通知ID',
  `target_no` varchar(64) DEFAULT NULL COMMENT '报告编号 / 通知单号',
  `analysis_id` bigint(20) DEFAULT NULL COMMENT '分析数据ID（报告专用）',
  `notify_type` varchar(20) DEFAULT NULL COMMENT '通知类型（delay/sample/wet_lab/bioinfo）',
  `subbarcode` varchar(128) DEFAULT NULL COMMENT '样本编号',
  `recipients` varchar(2000) NOT NULL COMMENT '收件人（分号分隔）',
  `recipient_count` bigint(20) NOT NULL DEFAULT '0' COMMENT '收件人数',
  `cc` varchar(2000) DEFAULT NULL COMMENT '抄送（分号分隔）',
  `subject` varchar(500) DEFAULT NULL COMMENT '邮件主题',
  `attachment_name` varchar(255) DEFAULT NULL COMMENT '附件文件名（不含路径）',
  `mail_config_id` bigint(20) DEFAULT NULL COMMENT '发件邮箱配置ID（report_mail_config.id）',
  `from_addr` varchar(200) DEFAULT NULL COMMENT '发件地址',
  `status` varchar(20) NOT NULL COMMENT 'SENDING / SUCCESS / FAILED',
  `provider_message_id` varchar(255) DEFAULT NULL COMMENT '邮件服务商 message-id',
  `failure_reason` varchar(1000) DEFAULT NULL COMMENT '失败原因摘要',
  `triggered_by` varchar(80) NOT NULL COMMENT '触发人',
  `requested_at` datetime NOT NULL COMMENT '发起时间',
  `finished_at` datetime DEFAULT NULL COMMENT '完成时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_email_trace` (`tenant_id`,`trace_id`),
  KEY `idx_delivery_email_target` (`target_type`,`target_id`,`requested_at`),
  KEY `idx_delivery_email_status` (`status`,`requested_at`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_delivery_email_subbarcode` (`tenant_id`,`subbarcode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交付物邮件发送记录（报告 + 通知）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `delivery_email_log`
--

/*!40000 ALTER TABLE `delivery_email_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `delivery_email_log` ENABLE KEYS */;

--
-- Table structure for table `dev_log`
--

DROP TABLE IF EXISTS `dev_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `dev_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(200) NOT NULL COMMENT '记录标题',
  `category` varchar(20) NOT NULL COMMENT '分类（feature 功能新增 / fix 缺陷修复 / change 变更调整）',
  `version` varchar(50) DEFAULT NULL COMMENT '版本号',
  `content` text COMMENT '详细内容',
  `file_name` varchar(255) DEFAULT NULL COMMENT '记录文档文件名（原文件名，附件存服务器固定目录）',
  `developer` varchar(80) DEFAULT NULL COMMENT '开发人员',
  `log_date` date DEFAULT NULL COMMENT '记录日期',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_dev_log_date` (`tenant_id`,`log_date`),
  KEY `idx_dev_log_category` (`tenant_id`,`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开发记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dev_log`
--

/*!40000 ALTER TABLE `dev_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `dev_log` ENABLE KEYS */;

--
-- Table structure for table `file_CNV`
--

DROP TABLE IF EXISTS `file_CNV`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_CNV` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
  `gene` varchar(255) DEFAULT NULL COMMENT '原字段：gene',
  `chr` varchar(50) DEFAULT NULL COMMENT '原字段：chr',
  `start` varchar(50) DEFAULT NULL COMMENT '原字段：start',
  `end` varchar(50) DEFAULT NULL COMMENT '原字段：end',
  `copy_num` varchar(50) DEFAULT NULL COMMENT '原字段：copy_num',
  `variant` varchar(50) DEFAULT NULL COMMENT '新增字段：variant',
  `ori_variant` varchar(50) DEFAULT NULL COMMENT '原字段：ori_variant',
  `parent_mutation_id` varchar(255) DEFAULT NULL COMMENT '人工关联的NKB父级mutation ID（逗号分隔，可多个）',
  `is_reported` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否纳入报告：0否，1是',
  `filtered_rationale` varchar(500) DEFAULT NULL COMMENT '过滤理由',
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_cnv_file` (`file_id`,`del_flag`),
  KEY `idx_file_cnv_parent_mutation` (`parent_mutation_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='CNV文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_CNV`
--

/*!40000 ALTER TABLE `file_CNV` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_CNV` ENABLE KEYS */;

--
-- Table structure for table `file_CR_ALL`
--

DROP TABLE IF EXISTS `file_CR_ALL`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_CR_ALL` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
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
  `parent_mutation_id` varchar(255) DEFAULT NULL COMMENT '人工关联的NKB父级mutation ID（逗号分隔，可多个）',
  `is_reported` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否纳入报告：0否，1是',
  `filtered_rationale` varchar(500) DEFAULT NULL COMMENT '过滤理由',
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_cr_all_file` (`file_id`,`del_flag`),
  KEY `idx_file_cr_parent_mutation` (`parent_mutation_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='CR_ALL文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_CR_ALL`
--

/*!40000 ALTER TABLE `file_CR_ALL` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_CR_ALL` ENABLE KEYS */;

--
-- Table structure for table `file_Chem`
--

DROP TABLE IF EXISTS `file_Chem`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_Chem` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
  `chr` varchar(50) DEFAULT NULL COMMENT '原字段：chr',
  `position` varchar(50) DEFAULT NULL COMMENT '原字段：position',
  `allele1` varchar(255) DEFAULT NULL COMMENT '原字段：allele1',
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_chem_file` (`file_id`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Chem文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_Chem`
--

/*!40000 ALTER TABLE `file_Chem` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_Chem` ENABLE KEYS */;

--
-- Table structure for table `file_Fusion`
--

DROP TABLE IF EXISTS `file_Fusion`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_Fusion` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
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
  `parent_mutation_id` varchar(255) DEFAULT NULL COMMENT '人工关联的NKB父级mutation ID（逗号分隔，可多个）',
  `is_reported` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否纳入报告：0否，1是',
  `filtered_rationale` varchar(500) DEFAULT NULL COMMENT '过滤理由',
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_fusion_file` (`file_id`,`del_flag`),
  KEY `idx_file_fusion_parent_mutation` (`parent_mutation_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='Fusion文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_Fusion`
--

/*!40000 ALTER TABLE `file_Fusion` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_Fusion` ENABLE KEYS */;

--
-- Table structure for table `file_MSI`
--

DROP TABLE IF EXISTS `file_MSI`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_MSI` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
  `score` varchar(255) DEFAULT NULL COMMENT '原字段：Score',
  `threshold` varchar(255) DEFAULT NULL COMMENT '原字段：Threshold',
  `status` varchar(255) DEFAULT NULL COMMENT '原字段：Status',
  `check_result` varchar(255) DEFAULT NULL COMMENT '原字段：checkResult',
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_msi_file` (`file_id`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='MSI文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_MSI`
--

/*!40000 ALTER TABLE `file_MSI` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_MSI` ENABLE KEYS */;

--
-- Table structure for table `file_Somatic_SNV_Indel`
--

DROP TABLE IF EXISTS `file_Somatic_SNV_Indel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_Somatic_SNV_Indel` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
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
  `parent_mutation_id` varchar(255) DEFAULT NULL COMMENT '人工关联的NKB父级mutation ID（逗号分隔，可多个）',
  `is_reported` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否纳入报告：0否，1是',
  `filtered_rationale` varchar(500) DEFAULT NULL COMMENT '过滤理由',
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_somatic_snv_file` (`file_id`,`del_flag`),
  KEY `idx_file_somatic_parent_mutation` (`parent_mutation_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='SomaticSNV文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_Somatic_SNV_Indel`
--

/*!40000 ALTER TABLE `file_Somatic_SNV_Indel` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_Somatic_SNV_Indel` ENABLE KEYS */;

--
-- Table structure for table `file_chemical`
--

DROP TABLE IF EXISTS `file_chemical`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_chemical` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
  `raw_json` longtext COMMENT '原始chemical.json完整内容',
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_chemical_file` (`file_id`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='chemical.json文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_chemical`
--

/*!40000 ALTER TABLE `file_chemical` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_chemical` ENABLE KEYS */;

--
-- Table structure for table `file_qc`
--

DROP TABLE IF EXISTS `file_qc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_qc` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
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
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_qc_file` (`file_id`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='qc文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_qc`
--

/*!40000 ALTER TABLE `file_qc` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_qc` ENABLE KEYS */;

--
-- Table structure for table `file_qc_control`
--

DROP TABLE IF EXISTS `file_qc_control`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `file_qc_control` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_id` bigint(20) NOT NULL COMMENT '关联analysis_file_status.file_id',
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
  `reviewed_by` bigint(20) DEFAULT NULL COMMENT '审核人ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_file_qc_file` (`file_id`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='对照样本qc文件明细';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `file_qc_control`
--

/*!40000 ALTER TABLE `file_qc_control` DISABLE KEYS */;
/*!40000 ALTER TABLE `file_qc_control` ENABLE KEYS */;

--
-- Table structure for table `gen_table`
--

DROP TABLE IF EXISTS `gen_table`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_table` (
  `table_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `data_name` varchar(200) DEFAULT '' COMMENT '数据源名称',
  `table_name` varchar(200) DEFAULT '' COMMENT '表名称',
  `table_comment` varchar(500) DEFAULT '' COMMENT '表描述',
  `sub_table_name` varchar(64) DEFAULT NULL COMMENT '关联子表的表名',
  `sub_table_fk_name` varchar(64) DEFAULT NULL COMMENT '子表关联的外键名',
  `class_name` varchar(100) DEFAULT '' COMMENT '实体类名称',
  `tpl_category` varchar(200) DEFAULT 'crud' COMMENT '使用的模板（crud单表操作 tree树表操作）',
  `package_name` varchar(100) DEFAULT NULL COMMENT '生成包路径',
  `module_name` varchar(30) DEFAULT NULL COMMENT '生成模块名',
  `business_name` varchar(30) DEFAULT NULL COMMENT '生成业务名',
  `function_name` varchar(50) DEFAULT NULL COMMENT '生成功能名',
  `function_author` varchar(50) DEFAULT NULL COMMENT '生成功能作者',
  `gen_type` char(1) DEFAULT '0' COMMENT '生成代码方式（0zip压缩包 1自定义路径）',
  `gen_path` varchar(200) DEFAULT '/' COMMENT '生成路径（不填默认项目路径）',
  `options` varchar(1000) DEFAULT NULL COMMENT '其它生成选项',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`table_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table`
--

/*!40000 ALTER TABLE `gen_table` DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table` ENABLE KEYS */;

--
-- Table structure for table `gen_table_column`
--

DROP TABLE IF EXISTS `gen_table_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `gen_table_column` (
  `column_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '编号',
  `table_id` bigint(20) DEFAULT NULL COMMENT '归属表编号',
  `column_name` varchar(200) DEFAULT NULL COMMENT '列名称',
  `column_comment` varchar(500) DEFAULT NULL COMMENT '列描述',
  `column_type` varchar(100) DEFAULT NULL COMMENT '列类型',
  `java_type` varchar(500) DEFAULT NULL COMMENT 'JAVA类型',
  `java_field` varchar(200) DEFAULT NULL COMMENT 'JAVA字段名',
  `is_pk` char(1) DEFAULT NULL COMMENT '是否主键（1是）',
  `is_increment` char(1) DEFAULT NULL COMMENT '是否自增（1是）',
  `is_required` char(1) DEFAULT NULL COMMENT '是否必填（1是）',
  `is_insert` char(1) DEFAULT NULL COMMENT '是否为插入字段（1是）',
  `is_edit` char(1) DEFAULT NULL COMMENT '是否编辑字段（1是）',
  `is_list` char(1) DEFAULT NULL COMMENT '是否列表字段（1是）',
  `is_query` char(1) DEFAULT NULL COMMENT '是否查询字段（1是）',
  `query_type` varchar(200) DEFAULT 'EQ' COMMENT '查询方式（等于、不等于、大于、小于、范围）',
  `html_type` varchar(200) DEFAULT NULL COMMENT '显示类型（文本框、文本域、下拉框、复选框、单选框、日期控件）',
  `dict_type` varchar(200) DEFAULT '' COMMENT '字典类型',
  `sort` int(11) DEFAULT NULL COMMENT '排序',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`column_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='代码生成业务表字段';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `gen_table_column`
--

/*!40000 ALTER TABLE `gen_table_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `gen_table_column` ENABLE KEYS */;

--
-- Table structure for table `history_germline`
--

DROP TABLE IF EXISTS `history_germline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `history_germline` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `match_key` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化匹配条件SHA-256',
  `key_version` tinyint(4) NOT NULL DEFAULT '1' COMMENT '复用键口径版本：1=旧键(含customer)，2=按产品项目+位点+癌种+性别+改靶父级',
  `gene` varchar(100) NOT NULL,
  `variant` varchar(500) NOT NULL,
  `ori_variant` varchar(1000) NOT NULL,
  `disease_id` int(11) DEFAULT NULL COMMENT 'NKB癌种ID',
  `disease` varchar(255) NOT NULL,
  `gender` varchar(16) NOT NULL,
  `customer` varchar(255) NOT NULL,
  `project_code` varchar(120) NOT NULL COMMENT 'analysis_data.product',
  `source_type` varchar(32) NOT NULL DEFAULT 'CR_ALL',
  `mutation_type` varchar(8) NOT NULL DEFAULT 'G',
  `measurement` varchar(100) DEFAULT NULL COMMENT '胚系合子状态',
  `parent_mutation_id` varchar(255) DEFAULT NULL COMMENT '人工改靶NKB mutation ID（逗号分隔，可多个）',
  `match_status` varchar(32) NOT NULL,
  `variation_class` varchar(32) DEFAULT NULL,
  `clinical_significance` tinyint(4) NOT NULL DEFAULT '3' COMMENT '临床意义：1致病，2可能致病，3未知临床意义，4可能良性，5良性',
  `match_result` json NOT NULL COMMENT '冻结的ReportDrugPreview.Item JSON',
  `last_reused_at` datetime(3) DEFAULT NULL COMMENT '最近一次被复用的时间',
  `reuse_count` int(11) NOT NULL DEFAULT '0' COMMENT '被复用次数',
  `source_analysis_id` bigint(20) NOT NULL,
  `source_report_id` bigint(20) NOT NULL,
  `source_variant_id` bigint(20) NOT NULL,
  `knowledge_matched_at` datetime(3) NOT NULL COMMENT '首次按知识库匹配时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_history_germline_match_key` (`tenant_id`,`match_key`),
  KEY `idx_history_germline_project` (`customer`(64),`project_code`(64),`disease_id`,`gender`),
  KEY `idx_history_germline_variant` (`gene`,`variant`(120),`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='胚系位点药物匹配历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `history_germline`
--

/*!40000 ALTER TABLE `history_germline` DISABLE KEYS */;
/*!40000 ALTER TABLE `history_germline` ENABLE KEYS */;

--
-- Table structure for table `history_reuse_log`
--

DROP TABLE IF EXISTS `history_reuse_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `history_reuse_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `history_type` varchar(16) NOT NULL COMMENT 'SOMATIC / GERMLINE',
  `history_id` bigint(20) NOT NULL COMMENT 'history_somatic / history_germline.id',
  `match_key` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化匹配键',
  `gene` varchar(100) NOT NULL COMMENT '基因',
  `variant` varchar(500) NOT NULL COMMENT '规范化位点',
  `project_code` varchar(120) NOT NULL COMMENT 'analysis_data.product',
  `report_id` bigint(20) NOT NULL COMMENT '复用时的报告ID',
  `analysis_id` bigint(20) NOT NULL COMMENT '复用时的分析批次ID',
  `source_variant_id` bigint(20) DEFAULT NULL COMMENT '复用时的来源位点ID',
  `reused_by` bigint(20) DEFAULT NULL COMMENT '操作人 sys_user.user_id',
  `reused_at` datetime(3) NOT NULL COMMENT '复用时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_history_reuse_log_history` (`history_type`,`history_id`,`reused_at`),
  KEY `idx_history_reuse_log_report` (`report_id`,`reused_at`),
  KEY `idx_history_reuse_log_match_key` (`match_key`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='位点匹配历史复用留痕';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `history_reuse_log`
--

/*!40000 ALTER TABLE `history_reuse_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `history_reuse_log` ENABLE KEYS */;

--
-- Table structure for table `history_somatic`
--

DROP TABLE IF EXISTS `history_somatic`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `history_somatic` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `match_key` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化匹配条件SHA-256',
  `key_version` tinyint(4) NOT NULL DEFAULT '1' COMMENT '复用键口径版本：1=旧键(含customer)，2=按产品项目+位点+癌种+性别+改靶父级',
  `gene` varchar(100) NOT NULL,
  `variant` varchar(500) NOT NULL,
  `ori_variant` varchar(1000) NOT NULL,
  `disease_id` int(11) DEFAULT NULL COMMENT 'NKB癌种ID',
  `disease` varchar(255) NOT NULL,
  `gender` varchar(16) NOT NULL,
  `customer` varchar(255) NOT NULL,
  `project_code` varchar(120) NOT NULL COMMENT 'analysis_data.product',
  `source_type` varchar(32) NOT NULL COMMENT 'SNP_INDEL/CNV/FUSION',
  `mutation_type` varchar(8) NOT NULL DEFAULT 'S',
  `measurement` varchar(100) DEFAULT NULL,
  `parent_mutation_id` varchar(255) DEFAULT NULL COMMENT '人工改靶NKB mutation ID（逗号分隔，可多个）',
  `match_status` varchar(32) NOT NULL,
  `variation_class` varchar(32) DEFAULT NULL,
  `match_result` json NOT NULL COMMENT '冻结的ReportDrugPreview.Item JSON',
  `last_reused_at` datetime(3) DEFAULT NULL COMMENT '最近一次被复用的时间',
  `reuse_count` int(11) NOT NULL DEFAULT '0' COMMENT '被复用次数',
  `source_analysis_id` bigint(20) NOT NULL,
  `source_report_id` bigint(20) NOT NULL,
  `source_variant_id` bigint(20) NOT NULL,
  `knowledge_matched_at` datetime(3) NOT NULL COMMENT '首次按知识库匹配时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_history_somatic_match_key` (`tenant_id`,`match_key`),
  KEY `idx_history_somatic_project` (`customer`(64),`project_code`(64),`disease_id`,`gender`),
  KEY `idx_history_somatic_variant` (`gene`,`variant`(120),`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='体系位点药物匹配历史';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `history_somatic`
--

/*!40000 ALTER TABLE `history_somatic` DISABLE KEYS */;
/*!40000 ALTER TABLE `history_somatic` ENABLE KEYS */;

--
-- Table structure for table `product_config`
--

DROP TABLE IF EXISTS `product_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_code` varchar(60) NOT NULL COMMENT '产品编码',
  `trade_name` varchar(120) DEFAULT NULL COMMENT '商品名',
  `product_bio` varchar(120) NOT NULL COMMENT '生信编码（= 上传目录第 2 层 product，与 analysis_data.product_bio 逐字相等）',
  `product_name` varchar(120) NOT NULL COMMENT '产品名称（系统用）',
  `related_diseases` varchar(500) DEFAULT NULL COMMENT '适应症',
  `test_type` varchar(60) DEFAULT NULL COMMENT '样本类型（组织 / 血液）',
  `control_type` varchar(60) DEFAULT NULL COMMENT '对照（血液 / PBMC / 无）',
  `product_version` varchar(30) DEFAULT NULL COMMENT '版本（LDT / IVD）',
  `sequencing_platform` varchar(60) DEFAULT NULL COMMENT '测序平台（surf5000 Dx 等）',
  `gene_count` int(11) DEFAULT NULL COMMENT '基因数/DNA（panel 基因个数，如 1238）',
  `report_cycle_days` int(11) DEFAULT NULL COMMENT '报告周期（天）',
  `maintain_lab` varchar(60) DEFAULT NULL COMMENT '维护实验室',
  `related_process` varchar(100) DEFAULT NULL COMMENT '关联工序（如 全流程）',
  `probe_type` varchar(120) DEFAULT NULL COMMENT '探针类型（如 正常1238探针 / 正常1238探针+UMI）',
  `panel_type` varchar(60) DEFAULT NULL COMMENT 'panel类型（DNA / RNA / DNA_RNA / WES / HRD 等，图里没有，冗余保留）',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态（active 启用 / inactive 停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_config_product_code` (`tenant_id`,`product_code`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_product_bio` (`tenant_id`,`product_bio`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品配置';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_config`
--

/*!40000 ALTER TABLE `product_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `product_config` ENABLE KEYS */;

--
-- Table structure for table `product_gene`
--

DROP TABLE IF EXISTS `product_gene`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_gene` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint(20) NOT NULL COMMENT '产品配置 id（product_config.id）',
  `gene_id` int(10) unsigned NOT NULL COMMENT '基因 id（nkb.ncbi_gene.gene_id）',
  `gene_symbol` varchar(24) NOT NULL COMMENT '基因符号（冗余自 nkb.ncbi_gene，展示时不必跨库 join）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_gene` (`tenant_id`,`product_id`,`gene_id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_product_gene_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品关联基因';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_gene`
--

/*!40000 ALTER TABLE `product_gene` DISABLE KEYS */;
/*!40000 ALTER TABLE `product_gene` ENABLE KEYS */;

--
-- Table structure for table `product_template`
--

DROP TABLE IF EXISTS `product_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `product_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint(20) NOT NULL COMMENT '逻辑关联product_config.id（本仓产品主数据表）',
  `template_id` bigint(20) NOT NULL COMMENT '逻辑关联report_template.template_id',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否默认模板',
  `enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '展示顺序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_product_template` (`tenant_id`,`product_id`,`template_id`),
  KEY `idx_product_template_template` (`template_id`,`enabled`),
  KEY `idx_product_template_product` (`product_id`,`enabled`,`sort_order`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='产品与报告模板多对多';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product_template`
--

/*!40000 ALTER TABLE `product_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `product_template` ENABLE KEYS */;

--
-- Table structure for table `project_config`
--

DROP TABLE IF EXISTS `project_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `project_config` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `parent_id` bigint(20) NOT NULL DEFAULT '0' COMMENT '父节点ID（0=项目行；研究中心行=所属项目行 id）',
  `project_no` varchar(60) NOT NULL COMMENT '项目编号（项目行唯一；中心行冗余存）',
  `project_name` varchar(200) NOT NULL DEFAULT '' COMMENT '项目名称（仅项目行有值）',
  `customer` varchar(200) NOT NULL DEFAULT '' COMMENT '客户名称（仅项目行有值）',
  `contract_name` varchar(200) DEFAULT NULL COMMENT '合同名称（仅项目行有值；展示字段，不参与业务逻辑）',
  `contract_no` varchar(100) DEFAULT NULL COMMENT '合同编号（仅项目行有值；展示字段）',
  `scheme_no` varchar(100) DEFAULT NULL COMMENT '研究方案编号（仅项目行有值；展示字段）',
  `research_center_no` varchar(100) NOT NULL DEFAULT '' COMMENT '研究中心编号（仅中心行有值；项目行为空串，与名称成对参与唯一键）',
  `research_center_name` varchar(200) NOT NULL DEFAULT '' COMMENT '研究中心名称（仅中心行有值；项目行为空串，参与唯一键）',
  `receive_email` varchar(500) NOT NULL DEFAULT '' COMMENT '收件邮箱（;分隔）；项目行=项目自身，中心行=该中心',
  `project_notify_email` varchar(500) NOT NULL DEFAULT '' COMMENT '项目通知收件邮箱（;分隔；仅项目行使用，与报告收件邮箱分开）',
  `cc_email` varchar(500) DEFAULT NULL COMMENT '抄送邮箱（;分隔）；项目行=项目自身，中心行=该中心',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_config_node` (`tenant_id`,`project_no`,`research_center_no`,`research_center_name`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_project_config_parent` (`tenant_id`,`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目配置（项目行 + 研究中心行，parent_id 自关联）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `project_config`
--

/*!40000 ALTER TABLE `project_config` DISABLE KEYS */;
/*!40000 ALTER TABLE `project_config` ENABLE KEYS */;

--
-- Table structure for table `qc_standard`
--

DROP TABLE IF EXISTS `qc_standard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `qc_standard` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint(20) NOT NULL COMMENT '关联产品（product_config.id）',
  `qc_item` varchar(100) NOT NULL COMMENT '质控项目名称（中文展示名）',
  `qc_param` varchar(100) DEFAULT NULL COMMENT '质控参数（英文原始字段名，判定时与生信质控实际值比对；中文名见 qc_item）',
  `qc_category` varchar(20) NOT NULL DEFAULT 'wet_lab' COMMENT '质控类别（wet_lab 湿实验 / bioinfo 生信）',
  `min_value` varchar(50) DEFAULT NULL COMMENT '合格下限（NULL 表示不设下限）',
  `max_value` varchar(50) DEFAULT NULL COMMENT '合格上限（NULL 表示不设上限）',
  `unit` varchar(30) DEFAULT NULL COMMENT '单位（如 %、X）',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态（active 启用 / inactive 停用）',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_qc_standard_product_item_category` (`tenant_id`,`product_id`,`qc_item`,`qc_category`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质控标准';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `qc_standard`
--

/*!40000 ALTER TABLE `qc_standard` DISABLE KEYS */;
/*!40000 ALTER TABLE `qc_standard` ENABLE KEYS */;

--
-- Table structure for table `report_template`
--

DROP TABLE IF EXISTS `report_template`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `report_template` (
  `template_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_code` varchar(120) NOT NULL COMMENT '稳定模板编码',
  `template_name` varchar(255) NOT NULL COMMENT '模板名称',
  `template_version` varchar(40) NOT NULL DEFAULT 'v1' COMMENT '模板版本',
  `customer_code` varchar(120) DEFAULT NULL COMMENT '客户编码',
  `project_id` bigint(20) DEFAULT NULL COMMENT '逻辑关联project_config.id（模板归属项目）',
  `report_type` varchar(40) NOT NULL COMMENT '报告类型',
  `module_code` varchar(500) DEFAULT NULL COMMENT '有序个性化Java报告模块编码列表，分号分隔；公共模块无需配置',
  `report_name` varchar(500) DEFAULT NULL COMMENT '报告命名模板：静态文本 + {{路径}} 动态取值；空=用默认命名',
  `template_path` varchar(1000) NOT NULL COMMENT '模板文件相对路径（相对 report.renderer.templates-root，可带子目录，如 xxx.docx / notify/xxx.docx）',
  `template_sha256` char(64) DEFAULT NULL COMMENT '模板文件SHA-256',
  `status` varchar(20) NOT NULL DEFAULT 'active' COMMENT '状态（active 启用 / inactive 停用）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`template_id`),
  UNIQUE KEY `uk_report_template_code` (`tenant_id`,`template_code`),
  KEY `idx_report_template_module` (`module_code`,`status`,`del_flag`),
  KEY `idx_report_template_customer` (`customer_code`,`status`,`del_flag`),
  KEY `idx_report_template_project` (`project_id`,`status`,`del_flag`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC COMMENT='报告模板';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `report_template`
--

/*!40000 ALTER TABLE `report_template` DISABLE KEYS */;
/*!40000 ALTER TABLE `report_template` ENABLE KEYS */;

--
-- Table structure for table `sample_notify`
--

DROP TABLE IF EXISTS `sample_notify`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sample_notify` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `notify_no` varchar(64) NOT NULL COMMENT '通知单号（首次生成：样本编号-yyyyMMddHHmmss；同日重生成不变）',
  `notify_type` varchar(20) NOT NULL DEFAULT 'bioinfo' COMMENT '通知类型（delay 延期通知 / sample 样本不合格 / wet_lab 湿实验不合格 / bioinfo 干实验不合格）',
  `notify_status` varchar(20) NOT NULL DEFAULT 'generated' COMMENT '通知状态（generated 已生成待提交 / pending 待处理 / sent 发送成功 / failed 发送失败）',
  `sign_status` varchar(20) NOT NULL DEFAULT 'none' COMMENT '签署状态（none 未送签 / submitted 已送签 / signed 已签署）',
  `notify_date` char(8) NOT NULL COMMENT '通知日期/出具日期（生成当天 yyyyMMdd）',
  `subbarcode` varchar(128) NOT NULL COMMENT '样本编号（= cms_sample.subbarcode）',
  `sample_name` varchar(100) DEFAULT NULL COMMENT '样本名称（快照，用于文件命名）',
  `scheme_no` varchar(128) DEFAULT NULL COMMENT '研究方案编号（= cms_sample.scheme_no）',
  `project_no` varchar(128) DEFAULT NULL COMMENT '项目编号（= cms_sample.project_no）',
  `subject_no` varchar(128) DEFAULT NULL COMMENT '试验参与者编号（= cms_sample.subject_no）',
  `product_name` varchar(256) DEFAULT NULL COMMENT '产品名称（= cms_sample.product_name）',
  `customer_name` varchar(100) DEFAULT NULL COMMENT '客户',
  `sample_type` varchar(100) DEFAULT NULL COMMENT '样本类型',
  `reason` varchar(500) DEFAULT NULL COMMENT '不合格原因（邮件正文要用）',
  `delay_reason` varchar(500) DEFAULT NULL COMMENT '检测报告延迟原因（delay 专用，邮件正文要用）',
  `expected_report_date` varchar(32) DEFAULT NULL COMMENT '预计报告出具时间（delay 专用，邮件正文要用）',
  `json_path` varchar(500) DEFAULT NULL COMMENT '渲染用 JSON 路径（容器内绝对路径）',
  `raw_docx_path` varchar(500) DEFAULT NULL COMMENT '生成的 DOCX 路径',
  `pdf_path` varchar(500) DEFAULT NULL COMMENT 'PDF 路径（OnlyOffice 转换产物）',
  `signed_pdf_path` varchar(500) DEFAULT NULL COMMENT '签名后 PDF 路径（上上签回传落盘）',
  `file_name` varchar(255) DEFAULT NULL COMMENT '交付文件名（不含路径；路径见四个 *_path）',
  `sign_contract_id` varchar(64) DEFAULT NULL COMMENT '上上签合同号（送签后回填）',
  `handler_by` bigint(20) DEFAULT NULL COMMENT '处理人 = 生成该通知的人（sys_user.user_id）',
  `handler_time` datetime DEFAULT NULL COMMENT '处理时间 = 生成时间',
  `sender_by` bigint(20) DEFAULT NULL COMMENT '发送人（sys_user.user_id）',
  `sender_time` datetime DEFAULT NULL COMMENT '发送时间',
  `send_to` varchar(500) DEFAULT NULL COMMENT '实际收件人（发送时快照，;分隔）',
  `send_cc` varchar(500) DEFAULT NULL COMMENT '实际抄送（发送时快照，;分隔）',
  `send_error` varchar(500) DEFAULT NULL COMMENT '最近一次发送失败原因（成功时清空）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_sample_notify_subbarcode` (`tenant_id`,`subbarcode`),
  KEY `idx_sample_notify_day` (`tenant_id`,`subbarcode`,`notify_type`,`notify_date`),
  KEY `idx_sample_notify_status` (`tenant_id`,`notify_status`,`notify_date`),
  KEY `idx_sample_notify_project` (`tenant_id`,`project_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='样本通知台账（延期/样本不合格/湿实验不合格/干实验不合格）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sample_notify`
--

/*!40000 ALTER TABLE `sample_notify` DISABLE KEYS */;
/*!40000 ALTER TABLE `sample_notify` ENABLE KEYS */;

--
-- Table structure for table `sys_client`
--

DROP TABLE IF EXISTS `sys_client`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_client` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'id',
  `client_id` varchar(64) DEFAULT NULL COMMENT '客户端id',
  `client_key` varchar(32) DEFAULT NULL COMMENT '客户端key',
  `client_secret` varchar(255) DEFAULT NULL COMMENT '客户端秘钥',
  `grant_type` varchar(255) DEFAULT NULL COMMENT '授权类型',
  `device_type` varchar(32) DEFAULT NULL COMMENT '设备类型',
  `active_timeout` int(11) DEFAULT '1800' COMMENT 'token活跃超时时间',
  `timeout` int(11) DEFAULT '604800' COMMENT 'token固定超时',
  `status` char(1) DEFAULT '0' COMMENT '状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='系统授权表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_client`
--

/*!40000 ALTER TABLE `sys_client` DISABLE KEYS */;
INSERT INTO `sys_client` (`id`, `client_id`, `client_key`, `client_secret`, `grant_type`, `device_type`, `active_timeout`, `timeout`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'e5cd7e4891bf95d1d19206ce24a7b32e','pc','pc123','password,social','pc',1800,604800,'0','0',1,'2026-11-03 20:28:27',1,'2026-11-03 20:28:27'),(2,'428a8310cd442757ae699df5d894f051','app','app123','password,sms,social','android',1800,604800,'0','0',1,'2026-11-03 20:28:27',1,'2026-11-03 20:28:27');
/*!40000 ALTER TABLE `sys_client` ENABLE KEYS */;

--
-- Table structure for table `sys_config`
--

DROP TABLE IF EXISTS `sys_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_config` (
  `config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '参数主键',
  `config_name` varchar(100) DEFAULT '' COMMENT '参数名称',
  `config_key` varchar(100) DEFAULT '' COMMENT '参数键名',
  `config_value` varchar(500) DEFAULT '' COMMENT '参数键值',
  `config_type` char(1) DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COMMENT='参数配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_config`
--

/*!40000 ALTER TABLE `sys_config` DISABLE KEYS */;
INSERT INTO `sys_config` (`config_id`, `config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'主框架页-默认皮肤样式名称','sys.index.skinName','skin-blue','Y',1,'2026-11-03 20:28:27',NULL,NULL,'蓝色 skin-blue、绿色 skin-green、紫色 skin-purple、红色 skin-red、黄色 skin-yellow'),(2,'用户管理-账号初始密码','sys.user.initPassword','123456','Y',1,'2026-11-03 20:28:27',NULL,NULL,'初始化密码 123456'),(3,'主框架页-侧边栏主题','sys.index.sideTheme','theme-dark','Y',1,'2026-11-03 20:28:27',NULL,NULL,'深色主题theme-dark，浅色主题theme-light'),(5,'账号自助-是否开启用户注册功能','sys.account.registerUser','false','Y',1,'2026-11-03 20:28:27',NULL,NULL,'是否开启注册用户功能（true开启，false关闭）'),(11,'OSS预览列表资源开关','sys.oss.previewListResource','true','Y',1,'2026-11-03 20:28:27',NULL,NULL,'true:开启, false:关闭');
/*!40000 ALTER TABLE `sys_config` ENABLE KEYS */;

--
-- Table structure for table `sys_dict_data`
--

DROP TABLE IF EXISTS `sys_dict_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dict_data` (
  `dict_code` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典编码',
  `dict_sort` int(4) DEFAULT '0' COMMENT '字典排序',
  `dict_label` varchar(100) DEFAULT '' COMMENT '字典标签',
  `dict_value` varchar(100) DEFAULT '' COMMENT '字典键值',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `css_class` varchar(100) DEFAULT NULL COMMENT '样式属性（其他样式扩展）',
  `list_class` varchar(100) DEFAULT NULL COMMENT '表格回显样式',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认（Y是 N否）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_code`)
) ENGINE=InnoDB AUTO_INCREMENT=39 DEFAULT CHARSET=utf8mb4 COMMENT='字典数据表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_data`
--

/*!40000 ALTER TABLE `sys_dict_data` DISABLE KEYS */;
INSERT INTO `sys_dict_data` (`dict_code`, `dict_sort`, `dict_label`, `dict_value`, `dict_type`, `css_class`, `list_class`, `is_default`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,1,'男','0','sys_user_sex','','','Y',1,'2026-11-03 20:28:27',NULL,NULL,'性别男'),(2,2,'女','1','sys_user_sex','','','N',1,'2026-11-03 20:28:27',NULL,NULL,'性别女'),(3,3,'未知','2','sys_user_sex','','','N',1,'2026-11-03 20:28:27',NULL,NULL,'性别未知'),(4,1,'显示','0','sys_show_hide','','success','Y',1,'2026-11-03 20:28:27',NULL,NULL,'显示菜单'),(5,2,'隐藏','1','sys_show_hide','','error','N',1,'2026-11-03 20:28:27',NULL,NULL,'隐藏菜单'),(6,1,'正常','0','sys_normal_disable','','success','Y',1,'2026-11-03 20:28:27',NULL,NULL,'正常状态'),(7,2,'停用','1','sys_normal_disable','','error','N',1,'2026-11-03 20:28:27',NULL,NULL,'停用状态'),(12,1,'是','Y','sys_yes_no','','success','Y',1,'2026-11-03 20:28:27',NULL,NULL,'系统默认是'),(13,2,'否','N','sys_yes_no','','error','N',1,'2026-11-03 20:28:27',NULL,NULL,'系统默认否'),(14,1,'通知','1','sys_notice_type','','warning','Y',1,'2026-11-03 20:28:27',NULL,NULL,'通知'),(15,2,'公告','2','sys_notice_type','','success','N',1,'2026-11-03 20:28:27',NULL,NULL,'公告'),(16,1,'正常','0','sys_notice_status','','success','Y',1,'2026-11-03 20:28:27',NULL,NULL,'正常状态'),(17,2,'关闭','1','sys_notice_status','','error','N',1,'2026-11-03 20:28:27',NULL,NULL,'关闭状态'),(18,1,'新增','1','sys_oper_type','','info','N',1,'2026-11-03 20:28:27',NULL,NULL,'新增操作'),(19,2,'修改','2','sys_oper_type','','info','N',1,'2026-11-03 20:28:27',NULL,NULL,'修改操作'),(20,3,'删除','3','sys_oper_type','','error','N',1,'2026-11-03 20:28:27',NULL,NULL,'删除操作'),(21,4,'授权','4','sys_oper_type','','info','N',1,'2026-11-03 20:28:27',NULL,NULL,'授权操作'),(22,5,'导出','5','sys_oper_type','','warning','N',1,'2026-11-03 20:28:27',NULL,NULL,'导出操作'),(23,6,'导入','6','sys_oper_type','','warning','N',1,'2026-11-03 20:28:27',NULL,NULL,'导入操作'),(24,7,'强退','7','sys_oper_type','','error','N',1,'2026-11-03 20:28:27',NULL,NULL,'强退操作'),(25,8,'生成代码','8','sys_oper_type','','warning','N',1,'2026-11-03 20:28:27',NULL,NULL,'生成操作'),(26,9,'清空数据','9','sys_oper_type','','error','N',1,'2026-11-03 20:28:27',NULL,NULL,'清空操作'),(27,1,'成功','0','sys_common_status','','success','N',1,'2026-11-03 20:28:27',NULL,NULL,'正常状态'),(28,2,'失败','1','sys_common_status','','error','N',1,'2026-11-03 20:28:27',NULL,NULL,'停用状态'),(29,99,'其他','0','sys_oper_type','','info','N',1,'2026-11-03 20:28:27',NULL,NULL,'其他操作'),(30,0,'密码认证','password','sys_grant_type','el-check-tag','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'密码认证'),(31,0,'短信认证','sms','sys_grant_type','el-check-tag','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'短信认证'),(32,0,'邮件认证','email','sys_grant_type','el-check-tag','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'邮件认证'),(33,0,'小程序认证','xcx','sys_grant_type','el-check-tag','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'小程序认证'),(34,0,'三方登录认证','social','sys_grant_type','el-check-tag','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'三方登录认证'),(35,0,'PC','pc','sys_device_type','','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'PC'),(36,0,'安卓','android','sys_device_type','','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'安卓'),(37,0,'iOS','ios','sys_device_type','','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'iOS'),(38,0,'小程序','xcx','sys_device_type','','default','N',1,'2026-11-03 20:28:27',NULL,NULL,'小程序');
/*!40000 ALTER TABLE `sys_dict_data` ENABLE KEYS */;

--
-- Table structure for table `sys_dict_type`
--

DROP TABLE IF EXISTS `sys_dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_dict_type` (
  `dict_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '字典主键',
  `dict_name` varchar(100) DEFAULT '' COMMENT '字典名称',
  `dict_type` varchar(100) DEFAULT '' COMMENT '字典类型',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`dict_id`),
  UNIQUE KEY `tenant_id` (`dict_type`),
  UNIQUE KEY `uk_sys_dict_type` (`dict_type`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COMMENT='字典类型表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_dict_type`
--

/*!40000 ALTER TABLE `sys_dict_type` DISABLE KEYS */;
INSERT INTO `sys_dict_type` (`dict_id`, `dict_name`, `dict_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'用户性别','sys_user_sex',1,'2026-11-03 20:28:27',NULL,NULL,'用户性别列表'),(2,'菜单状态','sys_show_hide',1,'2026-11-03 20:28:27',NULL,NULL,'菜单状态列表'),(3,'系统开关','sys_normal_disable',1,'2026-11-03 20:28:27',NULL,NULL,'系统开关列表'),(6,'系统是否','sys_yes_no',1,'2026-11-03 20:28:27',NULL,NULL,'系统是否列表'),(7,'通知类型','sys_notice_type',1,'2026-11-03 20:28:27',NULL,NULL,'通知类型列表'),(8,'通知状态','sys_notice_status',1,'2026-11-03 20:28:27',NULL,NULL,'通知状态列表'),(9,'操作类型','sys_oper_type',1,'2026-11-03 20:28:27',NULL,NULL,'操作类型列表'),(10,'系统状态','sys_common_status',1,'2026-11-03 20:28:27',NULL,NULL,'登录状态列表'),(11,'授权类型','sys_grant_type',1,'2026-11-03 20:28:27',NULL,NULL,'认证授权类型'),(12,'设备类型','sys_device_type',1,'2026-11-03 20:28:27',NULL,NULL,'客户端设备类型');
/*!40000 ALTER TABLE `sys_dict_type` ENABLE KEYS */;

--
-- Table structure for table `sys_logininfor`
--

DROP TABLE IF EXISTS `sys_logininfor`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_logininfor` (
  `info_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '访问ID',
  `user_name` varchar(50) DEFAULT '' COMMENT '用户账号',
  `client_key` varchar(32) DEFAULT '' COMMENT '客户端',
  `device_type` varchar(32) DEFAULT '' COMMENT '设备类型',
  `ipaddr` varchar(128) DEFAULT '' COMMENT '登录IP地址',
  `login_location` varchar(255) DEFAULT '' COMMENT '登录地点',
  `browser` varchar(50) DEFAULT '' COMMENT '浏览器类型',
  `os` varchar(50) DEFAULT '' COMMENT '操作系统',
  `status` char(1) DEFAULT '0' COMMENT '登录状态（0成功 1失败）',
  `msg` varchar(255) DEFAULT '' COMMENT '提示消息',
  `login_time` datetime DEFAULT NULL COMMENT '访问时间',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`info_id`),
  KEY `idx_sys_logininfor_s` (`status`),
  KEY `idx_sys_logininfor_lt` (`login_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统访问记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_logininfor`
--

/*!40000 ALTER TABLE `sys_logininfor` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_logininfor` ENABLE KEYS */;

--
-- Table structure for table `sys_menu`
--

DROP TABLE IF EXISTS `sys_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_menu` (
  `menu_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint(20) DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int(4) DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `query_param` varchar(255) DEFAULT NULL COMMENT '路由参数',
  `is_frame` int(1) DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int(1) DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '显示状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`menu_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1663 DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_menu`
--

/*!40000 ALTER TABLE `sys_menu` DISABLE KEYS */;
INSERT INTO `sys_menu` (`menu_id`, `menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'系统管理',0,1,'system',NULL,'',1,0,'M','0','0','','local-icon-system',1,'2026-11-03 20:28:27',NULL,NULL,'系统管理目录'),(2,'系统监控',0,3,'monitor',NULL,'',1,0,'M','0','0','','local-icon-monitor',1,'2026-11-03 20:28:27',NULL,NULL,'系统监控目录'),(3,'系统工具',0,4,'tool',NULL,'',1,0,'M','0','0','','local-icon-tool',1,'2026-11-03 20:28:27',NULL,NULL,'系统工具目录'),(4,'PLUS官网',0,5,'https://gitee.com/dromara/RuoYi-Vue-Plus',NULL,'',0,0,'M','0','0','','local-icon-guide',1,'2026-11-03 20:28:27',NULL,NULL,'RuoYi-Vue-Plus官网地址'),(100,'用户管理',1,1,'user','system/user/index','',1,0,'C','0','0','system:user:list','local-icon-user',1,'2026-11-03 20:28:27',NULL,NULL,'用户管理菜单'),(101,'角色管理',1,2,'role','system/role/index','',1,0,'C','0','0','system:role:list','local-icon-peoples',1,'2026-11-03 20:28:27',NULL,NULL,'角色管理菜单'),(102,'菜单管理',1,3,'menu','system/menu/index','',1,0,'C','0','0','system:menu:list','local-icon-tree-table',1,'2026-11-03 20:28:27',NULL,NULL,'菜单管理菜单'),(105,'字典管理',1,6,'dict','system/dict/index','',1,0,'C','0','0','system:dict:list','local-icon-dict',1,'2026-11-03 20:28:27',NULL,NULL,'字典管理菜单'),(106,'参数设置',1,7,'config','system/config/index','',1,0,'C','0','0','system:config:list','local-icon-edit',1,'2026-11-03 20:28:27',NULL,NULL,'参数设置菜单'),(107,'通知公告',1,8,'notice','system/notice/index','',1,0,'C','0','0','system:notice:list','local-icon-message',1,'2026-11-03 20:28:27',NULL,NULL,'通知公告菜单'),(108,'日志管理',1,9,'log','','',1,0,'M','0','0','','local-icon-log',1,'2026-11-03 20:28:27',NULL,NULL,'日志管理菜单'),(109,'在线用户',2,1,'online','monitor/online/index','',1,0,'C','0','0','monitor:online:list','local-icon-online',1,'2026-11-03 20:28:27',NULL,NULL,'在线用户菜单'),(113,'缓存监控',2,5,'cache','monitor/cache/index','',1,0,'C','0','0','monitor:cache:list','local-icon-redis',1,'2026-11-03 20:28:27',NULL,NULL,'缓存监控菜单'),(115,'代码生成',3,2,'gen','tool/gen/index','',1,0,'C','0','0','tool:gen:list','local-icon-code',1,'2026-11-03 20:28:27',NULL,NULL,'代码生成菜单'),(118,'文件管理',1,10,'oss','system/oss/index','',1,0,'C','0','0','system:oss:list','local-icon-upload',1,'2026-11-03 20:28:27',NULL,NULL,'文件管理菜单'),(123,'客户端管理',1,11,'client','system/client/index','',1,0,'C','0','0','system:client:list','local-icon-international',1,'2026-11-03 20:28:27',NULL,NULL,'客户端管理菜单'),(500,'操作日志',108,1,'operlog','monitor/operlog/index','',1,0,'C','0','0','monitor:operlog:list','local-icon-form',1,'2026-11-03 20:28:27',NULL,NULL,'操作日志菜单'),(501,'登录日志',108,2,'logininfor','monitor/logininfor/index','',1,0,'C','0','0','monitor:logininfor:list','local-icon-logininfor',1,'2026-11-03 20:28:27',NULL,NULL,'登录日志菜单'),(1001,'用户查询',100,1,'','','',1,0,'F','0','0','system:user:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1002,'用户新增',100,2,'','','',1,0,'F','0','0','system:user:add','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1003,'用户修改',100,3,'','','',1,0,'F','0','0','system:user:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1004,'用户删除',100,4,'','','',1,0,'F','0','0','system:user:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1005,'用户导出',100,5,'','','',1,0,'F','0','0','system:user:export','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1006,'用户导入',100,6,'','','',1,0,'F','0','0','system:user:import','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1007,'重置密码',100,7,'','','',1,0,'F','0','0','system:user:resetPwd','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1008,'角色查询',101,1,'','','',1,0,'F','0','0','system:role:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1009,'角色新增',101,2,'','','',1,0,'F','0','0','system:role:add','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1010,'角色修改',101,3,'','','',1,0,'F','0','0','system:role:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1011,'角色删除',101,4,'','','',1,0,'F','0','0','system:role:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1012,'角色导出',101,5,'','','',1,0,'F','0','0','system:role:export','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1013,'菜单查询',102,1,'','','',1,0,'F','0','0','system:menu:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1014,'菜单新增',102,2,'','','',1,0,'F','0','0','system:menu:add','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1015,'菜单修改',102,3,'','','',1,0,'F','0','0','system:menu:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1016,'菜单删除',102,4,'','','',1,0,'F','0','0','system:menu:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1026,'字典查询',105,1,'#','','',1,0,'F','0','0','system:dict:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1027,'字典新增',105,2,'#','','',1,0,'F','0','0','system:dict:add','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1028,'字典修改',105,3,'#','','',1,0,'F','0','0','system:dict:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1029,'字典删除',105,4,'#','','',1,0,'F','0','0','system:dict:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1030,'字典导出',105,5,'#','','',1,0,'F','0','0','system:dict:export','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1031,'参数查询',106,1,'#','','',1,0,'F','0','0','system:config:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1032,'参数新增',106,2,'#','','',1,0,'F','0','0','system:config:add','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1033,'参数修改',106,3,'#','','',1,0,'F','0','0','system:config:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1034,'参数删除',106,4,'#','','',1,0,'F','0','0','system:config:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1035,'参数导出',106,5,'#','','',1,0,'F','0','0','system:config:export','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1036,'公告查询',107,1,'#','','',1,0,'F','0','0','system:notice:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1037,'公告新增',107,2,'#','','',1,0,'F','0','0','system:notice:add','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1038,'公告修改',107,3,'#','','',1,0,'F','0','0','system:notice:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1039,'公告删除',107,4,'#','','',1,0,'F','0','0','system:notice:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1040,'操作查询',500,1,'#','','',1,0,'F','0','0','monitor:operlog:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1041,'操作删除',500,2,'#','','',1,0,'F','0','0','monitor:operlog:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1042,'日志导出',500,4,'#','','',1,0,'F','0','0','monitor:operlog:export','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1043,'登录查询',501,1,'#','','',1,0,'F','0','0','monitor:logininfor:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1044,'登录删除',501,2,'#','','',1,0,'F','0','0','monitor:logininfor:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1045,'日志导出',501,3,'#','','',1,0,'F','0','0','monitor:logininfor:export','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1046,'在线查询',109,1,'#','','',1,0,'F','0','0','monitor:online:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1047,'批量强退',109,2,'#','','',1,0,'F','0','0','monitor:online:batchLogout','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1048,'单条强退',109,3,'#','','',1,0,'F','0','0','monitor:online:forceLogout','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1050,'账户解锁',501,4,'#','','',1,0,'F','0','0','monitor:logininfor:unlock','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1055,'生成查询',115,1,'#','','',1,0,'F','0','0','tool:gen:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1056,'生成修改',115,2,'#','','',1,0,'F','0','0','tool:gen:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1057,'生成删除',115,3,'#','','',1,0,'F','0','0','tool:gen:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1058,'导入代码',115,2,'#','','',1,0,'F','0','0','tool:gen:import','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1059,'预览代码',115,4,'#','','',1,0,'F','0','0','tool:gen:preview','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1060,'生成代码',115,5,'#','','',1,0,'F','0','0','tool:gen:code','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1061,'客户端管理查询',123,1,'#','','',1,0,'F','0','0','system:client:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1062,'客户端管理新增',123,2,'#','','',1,0,'F','0','0','system:client:add','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1063,'客户端管理修改',123,3,'#','','',1,0,'F','0','0','system:client:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1064,'客户端管理删除',123,4,'#','','',1,0,'F','0','0','system:client:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1065,'客户端管理导出',123,5,'#','','',1,0,'F','0','0','system:client:export','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1600,'文件查询',118,1,'#','','',1,0,'F','0','0','system:oss:query','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1601,'文件上传',118,2,'#','','',1,0,'F','0','0','system:oss:upload','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1602,'文件下载',118,3,'#','','',1,0,'F','0','0','system:oss:download','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1603,'文件删除',118,4,'#','','',1,0,'F','0','0','system:oss:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1620,'配置列表',118,5,'#','','',1,0,'F','0','0','system:ossConfig:list','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1621,'配置添加',118,6,'#','','',1,0,'F','0','0','system:ossConfig:add','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1622,'配置编辑',118,6,'#','','',1,0,'F','0','0','system:ossConfig:edit','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1623,'配置删除',118,6,'#','','',1,0,'F','0','0','system:ossConfig:remove','#',1,'2026-11-03 20:28:27',NULL,NULL,''),(1624,'route.system_tenant',1,0,'tenant','system/tenant/index',NULL,1,0,'C','0','0','system:tenant:list','mdi:office-building-outline',1,'2026-11-03 20:28:28',NULL,NULL,'租户数据隔离管理'),(1625,'项目管理',0,2,'project','Layout','',1,1,'M','0','0','','local-icon-category',1,'2026-11-03 20:28:29',1,'2026-11-03 20:28:29',''),(1626,'合规管理',0,6,'compliance','Layout',NULL,1,0,'M','0','0','','local-icon-lock',1,'2026-11-03 20:28:29',1,'2026-11-03 20:28:29','合规管理（一级目录）'),(1627,'route.compliance_dev-log',1626,2,'dev-log','compliance/dev-log/index',NULL,1,0,'C','0','0','compliance:devLog:list','local-icon-log',1,'2026-11-03 20:28:29',1,'2026-11-03 20:28:29','开发记录'),(1628,'查询',1627,1,'',NULL,NULL,1,0,'F','0','0','compliance:devLog:query','#',1,'2026-11-03 20:28:29',NULL,NULL,'查询'),(1629,'新增',1627,2,'',NULL,NULL,1,0,'F','0','0','compliance:devLog:add','#',1,'2026-11-03 20:28:29',NULL,NULL,'新增'),(1630,'修改',1627,3,'',NULL,NULL,1,0,'F','0','0','compliance:devLog:edit','#',1,'2026-11-03 20:28:29',NULL,NULL,'修改'),(1631,'route.compliance_validation-records',1626,1,'validation-records','compliance/validation-records/index',NULL,1,0,'C','0','0','compliance:validationRecord:list','local-icon-my-task',1,'2026-11-03 20:28:29',1,'2026-11-03 20:28:29','3Q 验证记录'),(1632,'查询',1631,1,'',NULL,NULL,1,0,'F','0','0','compliance:validationRecord:query','#',1,'2026-11-03 20:28:29',NULL,NULL,'查询'),(1633,'新增',1631,2,'',NULL,NULL,1,0,'F','0','0','compliance:validationRecord:add','#',1,'2026-11-03 20:28:29',NULL,NULL,'新增'),(1634,'修改',1631,3,'',NULL,NULL,1,0,'F','0','0','compliance:validationRecord:edit','#',1,'2026-11-03 20:28:29',NULL,NULL,'修改'),(1635,'route.compliance_documents',1626,3,'documents','compliance/documents/index',NULL,1,0,'C','0','0','compliance:complianceDoc:list','local-icon-clipboard',1,'2026-11-03 20:28:29',1,'2026-11-03 20:28:29','3Q 文档管理'),(1636,'查询',1635,1,'',NULL,NULL,1,0,'F','0','0','compliance:complianceDoc:query','#',1,'2026-11-03 20:28:29',NULL,NULL,'查询'),(1637,'新增',1635,2,'',NULL,NULL,1,0,'F','0','0','compliance:complianceDoc:add','#',1,'2026-11-03 20:28:29',NULL,NULL,'新增'),(1638,'修改',1635,3,'',NULL,NULL,1,0,'F','0','0','compliance:complianceDoc:edit','#',1,'2026-11-03 20:28:29',NULL,NULL,'修改'),(1639,'删除',1635,4,'',NULL,NULL,1,0,'F','0','0','compliance:complianceDoc:remove','#',1,'2026-11-03 20:28:29',NULL,NULL,'删除'),(1640,'route.project_project-config',1625,1,'project-config','project/project-config/index',NULL,1,0,'C','0','0','project:projectConfig:list','local-icon-company',1,'2026-11-03 20:28:30',NULL,NULL,'项目配置'),(1641,'查询',1640,1,'',NULL,NULL,1,0,'F','0','0','project:projectConfig:query','#',1,'2026-11-03 20:28:30',NULL,NULL,'查询'),(1642,'新增',1640,2,'',NULL,NULL,1,0,'F','0','0','project:projectConfig:add','#',1,'2026-11-03 20:28:30',NULL,NULL,'新增'),(1643,'修改',1640,3,'',NULL,NULL,1,0,'F','0','0','project:projectConfig:edit','#',1,'2026-11-03 20:28:30',NULL,NULL,'修改'),(1644,'删除',1640,4,'',NULL,NULL,1,0,'F','0','0','project:projectConfig:remove','#',1,'2026-11-03 20:28:30',NULL,NULL,'删除'),(1645,'导出',1640,5,'',NULL,NULL,1,0,'F','0','0','project:projectConfig:export','#',1,'2026-11-03 20:28:30',NULL,NULL,'研究中心导出'),(1646,'导入',1640,6,'',NULL,NULL,1,0,'F','0','0','project:projectConfig:import','#',1,'2026-11-03 20:28:30',NULL,NULL,'研究中心导入'),(1647,'route.project_product-config',1625,2,'product-config','project/product-config/index',NULL,1,0,'C','0','0','project:productConfig:list','local-icon-company',1,'2026-11-03 20:28:30',NULL,NULL,'产品配置'),(1648,'查询',1647,1,'',NULL,NULL,1,0,'F','0','0','project:productConfig:query','#',1,'2026-11-03 20:28:30',NULL,NULL,'查询'),(1649,'新增',1647,2,'',NULL,NULL,1,0,'F','0','0','project:productConfig:add','#',1,'2026-11-03 20:28:30',NULL,NULL,'新增'),(1650,'修改',1647,3,'',NULL,NULL,1,0,'F','0','0','project:productConfig:edit','#',1,'2026-11-03 20:28:30',NULL,NULL,'修改'),(1651,'删除',1647,4,'',NULL,NULL,1,0,'F','0','0','project:productConfig:remove','#',1,'2026-11-03 20:28:30',NULL,NULL,'删除'),(1652,'样本管理',0,3,'qc','Layout',NULL,1,0,'M','0','0','','local-icon-skill',1,'2026-11-03 20:28:30',NULL,NULL,'样本管理（一级目录）'),(1653,'route.project_qc-standard',1625,3,'qc-standard','project/qc-standard/index',NULL,1,0,'C','0','0','qc:qcStandard:list','local-icon-dict',1,'2026-11-03 20:28:30',NULL,NULL,'质控标准'),(1654,'查询',1653,1,'',NULL,NULL,1,0,'F','0','0','qc:qcStandard:query','#',1,'2026-11-03 20:28:30',NULL,NULL,'查询'),(1655,'新增',1653,2,'',NULL,NULL,1,0,'F','0','0','qc:qcStandard:add','#',1,'2026-11-03 20:28:30',NULL,NULL,'新增'),(1656,'修改',1653,3,'',NULL,NULL,1,0,'F','0','0','qc:qcStandard:edit','#',1,'2026-11-03 20:28:30',NULL,NULL,'修改'),(1657,'删除',1653,4,'',NULL,NULL,1,0,'F','0','0','qc:qcStandard:remove','#',1,'2026-11-03 20:28:30',NULL,NULL,'删除'),(1658,'报告管理',0,3,'report',NULL,NULL,1,0,'M','0','0','','local-icon-post',1,'2026-11-03 20:28:31',NULL,NULL,'报告管理（目录）'),(1659,'查询',1641,1,'',NULL,NULL,1,0,'F','0','0','qc:bioinfo:query','#',1,'2026-11-03 20:28:31',NULL,NULL,'查询'),(1660,'接口接收',1641,2,'',NULL,NULL,1,0,'F','0','0','qc:bioinfo:receive','#',1,'2026-11-03 20:28:31',NULL,NULL,'接口接收'),(1661,'修改',1641,3,'',NULL,NULL,1,0,'F','0','0','qc:bioinfo:edit','#',1,'2026-11-03 20:28:31',NULL,NULL,'修改'),(1662,'删除',1641,4,'',NULL,NULL,1,0,'F','0','0','qc:bioinfo:remove','#',1,'2026-11-03 20:28:31',NULL,NULL,'删除');
/*!40000 ALTER TABLE `sys_menu` ENABLE KEYS */;

--
-- Table structure for table `sys_notice`
--

DROP TABLE IF EXISTS `sys_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_notice` (
  `notice_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '公告ID',
  `notice_title` varchar(50) NOT NULL COMMENT '公告标题',
  `notice_type` char(1) NOT NULL COMMENT '公告类型（1通知 2公告）',
  `notice_content` longblob COMMENT '公告内容',
  `status` char(1) DEFAULT '0' COMMENT '公告状态（0正常 1关闭）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`notice_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COMMENT='通知公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_notice`
--

/*!40000 ALTER TABLE `sys_notice` DISABLE KEYS */;
INSERT INTO `sys_notice` (`notice_id`, `notice_title`, `notice_type`, `notice_content`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `tenant_id`) VALUES (1,'温馨提醒：2018-07-01 新版本发布啦','2',_binary '新版本内容','0',1,'2026-11-03 20:28:27',NULL,NULL,'管理员','000000'),(2,'维护通知：2018-07-01 系统凌晨维护','1',_binary '维护内容','0',1,'2026-11-03 20:28:27',NULL,NULL,'管理员','000000');
/*!40000 ALTER TABLE `sys_notice` ENABLE KEYS */;

--
-- Table structure for table `sys_oper_log`
--

DROP TABLE IF EXISTS `sys_oper_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_oper_log` (
  `oper_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  `title` varchar(50) DEFAULT '' COMMENT '模块标题',
  `business_type` int(2) DEFAULT '0' COMMENT '业务类型（0其它 1新增 2修改 3删除）',
  `method` varchar(100) DEFAULT '' COMMENT '方法名称',
  `request_method` varchar(10) DEFAULT '' COMMENT '请求方式',
  `operator_type` int(1) DEFAULT '0' COMMENT '操作类别（0其它 1后台用户 2手机端用户）',
  `oper_name` varchar(50) DEFAULT '' COMMENT '操作人员',
  `dept_name` varchar(50) DEFAULT '' COMMENT '部门名称',
  `oper_url` varchar(255) DEFAULT '' COMMENT '请求URL',
  `oper_ip` varchar(128) DEFAULT '' COMMENT '主机地址',
  `oper_location` varchar(255) DEFAULT '' COMMENT '操作地点',
  `oper_param` varchar(4000) DEFAULT '' COMMENT '请求参数',
  `json_result` varchar(4000) DEFAULT '' COMMENT '返回参数',
  `status` int(1) DEFAULT '0' COMMENT '操作状态（0正常 1异常）',
  `error_msg` varchar(4000) DEFAULT '' COMMENT '错误消息',
  `oper_time` datetime DEFAULT NULL COMMENT '操作时间',
  `cost_time` bigint(20) DEFAULT '0' COMMENT '消耗时间',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`oper_id`),
  KEY `idx_sys_oper_log_bt` (`business_type`),
  KEY `idx_sys_oper_log_s` (`status`),
  KEY `idx_sys_oper_log_ot` (`oper_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oper_log`
--

/*!40000 ALTER TABLE `sys_oper_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oper_log` ENABLE KEYS */;

--
-- Table structure for table `sys_oss`
--

DROP TABLE IF EXISTS `sys_oss`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_oss` (
  `oss_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '对象存储主键',
  `file_name` varchar(255) NOT NULL DEFAULT '' COMMENT '文件名',
  `original_name` varchar(255) NOT NULL DEFAULT '' COMMENT '原名',
  `file_suffix` varchar(10) NOT NULL DEFAULT '' COMMENT '文件后缀名',
  `url` varchar(500) NOT NULL COMMENT 'URL地址',
  `ext1` text COMMENT '扩展字段',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '上传人',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新人',
  `service` varchar(20) NOT NULL DEFAULT 'minio' COMMENT '服务商',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`oss_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OSS对象存储表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oss`
--

/*!40000 ALTER TABLE `sys_oss` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_oss` ENABLE KEYS */;

--
-- Table structure for table `sys_oss_config`
--

DROP TABLE IF EXISTS `sys_oss_config`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_oss_config` (
  `oss_config_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key` varchar(20) NOT NULL DEFAULT '' COMMENT '配置key',
  `access_key` varchar(255) DEFAULT '' COMMENT 'accessKey',
  `secret_key` varchar(255) DEFAULT '' COMMENT '秘钥',
  `bucket_name` varchar(255) DEFAULT '' COMMENT '桶名称',
  `prefix` varchar(255) DEFAULT '' COMMENT '前缀',
  `endpoint` varchar(255) DEFAULT '' COMMENT '访问站点',
  `domain` varchar(255) DEFAULT '' COMMENT '自定义域名',
  `is_https` char(1) DEFAULT 'N' COMMENT '是否https（Y=是,N=否）',
  `region` varchar(255) DEFAULT '' COMMENT '域',
  `access_policy` char(1) NOT NULL DEFAULT '1' COMMENT '桶权限类型(0=private 1=public 2=custom)',
  `status` char(1) DEFAULT '1' COMMENT '是否默认（0=是,1=否）',
  `ext1` varchar(255) DEFAULT '' COMMENT '扩展字段',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`oss_config_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COMMENT='对象存储配置表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_oss_config`
--

/*!40000 ALTER TABLE `sys_oss_config` DISABLE KEYS */;
INSERT INTO `sys_oss_config` (`oss_config_id`, `config_key`, `access_key`, `secret_key`, `bucket_name`, `prefix`, `endpoint`, `domain`, `is_https`, `region`, `access_policy`, `status`, `ext1`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'minio','ruoyi','ruoyi123','ruoyi','','127.0.0.1:9000','','N','','1','0','',1,'2026-11-03 20:28:27',1,'2026-11-03 20:28:27',NULL),(2,'qiniu','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi','','s3-cn-north-1.qiniucs.com','','N','','1','1','',1,'2026-11-03 20:28:27',1,'2026-11-03 20:28:27',NULL),(3,'aliyun','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi','','oss-cn-beijing.aliyuncs.com','','N','','1','1','',1,'2026-11-03 20:28:27',1,'2026-11-03 20:28:27',NULL),(4,'qcloud','XXXXXXXXXXXXXXX','XXXXXXXXXXXXXXX','ruoyi-1240000000','','cos.ap-beijing.myqcloud.com','','N','ap-beijing','1','1','',1,'2026-11-03 20:28:27',1,'2026-11-03 20:28:27',NULL),(5,'image','ruoyi','ruoyi123','ruoyi','image','127.0.0.1:9000','','N','','1','1','',1,'2026-11-03 20:28:27',1,'2026-11-03 20:28:27',NULL);
/*!40000 ALTER TABLE `sys_oss_config` ENABLE KEYS */;

--
-- Table structure for table `sys_role`
--

DROP TABLE IF EXISTS `sys_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role` (
  `role_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_sort` int(4) NOT NULL COMMENT '显示顺序',
  `menu_check_strictly` tinyint(1) DEFAULT '1' COMMENT '菜单树选择项是否关联显示',
  `status` char(1) NOT NULL COMMENT '角色状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role`
--

/*!40000 ALTER TABLE `sys_role` DISABLE KEYS */;
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `menu_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES (1,'超级管理员','superadmin',1,1,'0','0',1,'2026-11-03 20:28:27',NULL,NULL,'超级管理员'),(3,'普通用户','test1',3,1,'0','0',1,'2026-11-03 20:28:27',NULL,NULL,''),(4,'业务用户','test2',4,1,'0','0',1,'2026-11-03 20:28:27',NULL,NULL,'');
/*!40000 ALTER TABLE `sys_role` ENABLE KEYS */;

--
-- Table structure for table `sys_role_menu`
--

DROP TABLE IF EXISTS `sys_role_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `menu_id` bigint(20) NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_role_menu`
--

/*!40000 ALTER TABLE `sys_role_menu` DISABLE KEYS */;
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1,1624),(1,1625),(1,1626),(1,1627),(1,1628),(1,1629),(1,1630),(1,1631),(1,1632),(1,1633),(1,1634),(1,1635),(1,1636),(1,1637),(1,1638),(1,1639),(1,1640),(1,1641),(1,1642),(1,1643),(1,1644),(1,1645),(1,1646),(1,1647),(1,1648),(1,1649),(1,1650),(1,1651),(1,1653),(1,1654),(1,1655),(1,1656),(1,1657),(1,1658),(1,1659),(1,1660),(1,1661),(1,1662),(3,1),(3,100),(3,101),(3,102),(3,105),(3,106),(3,107),(3,108),(3,118),(3,123),(3,500),(3,501),(3,1001),(3,1002),(3,1003),(3,1004),(3,1005),(3,1006),(3,1007),(3,1008),(3,1009),(3,1010),(3,1011),(3,1012),(3,1013),(3,1014),(3,1015),(3,1016),(3,1026),(3,1027),(3,1028),(3,1029),(3,1030),(3,1031),(3,1032),(3,1033),(3,1034),(3,1035),(3,1036),(3,1037),(3,1038),(3,1039),(3,1040),(3,1041),(3,1042),(3,1043),(3,1044),(3,1045),(3,1050),(3,1061),(3,1062),(3,1063),(3,1064),(3,1065),(3,1600),(3,1601),(3,1602),(3,1603),(3,1620),(3,1621),(3,1622),(3,1623);
/*!40000 ALTER TABLE `sys_role_menu` ENABLE KEYS */;

--
-- Table structure for table `sys_social`
--

DROP TABLE IF EXISTS `sys_social`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_social` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `auth_id` varchar(255) NOT NULL COMMENT '平台+平台唯一id',
  `source` varchar(255) NOT NULL COMMENT '用户来源',
  `open_id` varchar(255) DEFAULT NULL COMMENT '平台编号唯一id',
  `user_name` varchar(30) NOT NULL COMMENT '登录账号',
  `nick_name` varchar(30) DEFAULT '' COMMENT '用户昵称',
  `email` varchar(255) DEFAULT '' COMMENT '用户邮箱',
  `avatar` varchar(500) DEFAULT '' COMMENT '头像地址',
  `access_token` varchar(2000) NOT NULL COMMENT '用户的授权令牌',
  `expire_in` int(11) DEFAULT NULL COMMENT '用户的授权令牌的有效期，部分平台可能没有',
  `refresh_token` varchar(255) DEFAULT NULL COMMENT '刷新令牌，部分平台可能没有',
  `access_code` varchar(2000) DEFAULT NULL COMMENT '平台的授权信息，部分平台可能没有',
  `union_id` varchar(255) DEFAULT NULL COMMENT '用户的 unionid',
  `scope` varchar(255) DEFAULT NULL COMMENT '授予的权限，部分平台可能没有',
  `token_type` varchar(255) DEFAULT NULL COMMENT '个别平台的授权信息，部分平台可能没有',
  `id_token` varchar(2000) DEFAULT NULL COMMENT 'id token，部分平台可能没有',
  `mac_algorithm` varchar(255) DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
  `mac_key` varchar(255) DEFAULT NULL COMMENT '小米平台用户的附带属性，部分平台可能没有',
  `code` varchar(255) DEFAULT NULL COMMENT '用户的授权code，部分平台可能没有',
  `oauth_token` varchar(255) DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
  `oauth_token_secret` varchar(255) DEFAULT NULL COMMENT 'Twitter平台用户的附带属性，部分平台可能没有',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='社会化关系表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_social`
--

/*!40000 ALTER TABLE `sys_social` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_social` ENABLE KEYS */;

--
-- Table structure for table `sys_tenant`
--

DROP TABLE IF EXISTS `sys_tenant`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_tenant` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `tenant_id` varchar(20) NOT NULL,
  `tenant_name` varchar(100) NOT NULL,
  `status` char(1) NOT NULL DEFAULT '0',
  `remark` varchar(500) DEFAULT NULL,
  `create_by` bigint(20) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` bigint(20) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_tenant_identifier` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COMMENT='租户目录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_tenant`
--

/*!40000 ALTER TABLE `sys_tenant` DISABLE KEYS */;
INSERT INTO `sys_tenant` (`id`, `tenant_id`, `tenant_name`, `status`, `remark`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES (1,'000000','默认租户','0',NULL,1,'2026-11-03 20:28:28',NULL,NULL);
/*!40000 ALTER TABLE `sys_tenant` ENABLE KEYS */;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user` (
  `user_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name` varchar(30) NOT NULL COMMENT '用户账号',
  `nick_name` varchar(30) NOT NULL COMMENT '用户昵称',
  `user_type` varchar(10) DEFAULT 'sys_user' COMMENT '用户类型（sys_user系统用户）',
  `email` varchar(50) DEFAULT '' COMMENT '用户邮箱',
  `phonenumber` varchar(11) DEFAULT '' COMMENT '手机号码',
  `sex` char(1) DEFAULT '0' COMMENT '用户性别（0男 1女 2未知）',
  `avatar` bigint(20) DEFAULT NULL COMMENT '头像地址',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `status` char(1) DEFAULT '0' COMMENT '账号状态（0正常 1停用）',
  `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `login_ip` varchar(128) DEFAULT '' COMMENT '最后登录IP',
  `login_date` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `uk_sys_user_login` (`user_name`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` (`user_id`, `user_name`, `nick_name`, `user_type`, `email`, `phonenumber`, `sex`, `avatar`, `password`, `status`, `del_flag`, `login_ip`, `login_date`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`, `tenant_id`) VALUES (1,'admin','疯狂的狮子Li','sys_user','crazyLionLi@163.com','15888888888','1',NULL,'$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2','0','0','127.0.0.1','2026-11-03 20:28:27',1,'2026-11-03 20:28:27',NULL,NULL,'管理员','000000'),(3,'test','本部门及以下 密码666666','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1','2026-11-03 20:28:27',1,'2026-11-03 20:28:27',3,'2026-11-03 20:28:27',NULL,'000000'),(4,'test1','仅本人 密码666666','sys_user','','','0',NULL,'$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne','0','0','127.0.0.1','2026-11-03 20:28:27',1,'2026-11-03 20:28:27',4,'2026-11-03 20:28:27',NULL,'000000');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;

--
-- Table structure for table `sys_user_role`
--

DROP TABLE IF EXISTS `sys_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `sys_user_role` (
  `user_id` bigint(20) NOT NULL COMMENT '用户ID',
  `role_id` bigint(20) NOT NULL COMMENT '角色ID',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user_role`
--

/*!40000 ALTER TABLE `sys_user_role` DISABLE KEYS */;
INSERT INTO `sys_user_role` (`user_id`, `role_id`, `tenant_id`) VALUES (1,1,'000000'),(3,3,'000000'),(4,4,'000000');
/*!40000 ALTER TABLE `sys_user_role` ENABLE KEYS */;

--
-- Table structure for table `validation_record`
--

DROP TABLE IF EXISTS `validation_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `validation_record` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `validation_type` varchar(20) NOT NULL COMMENT '验证类型（IQ 安装确认 / OQ 运行确认 / PQ 性能确认）',
  `title` varchar(200) NOT NULL COMMENT '验证标题',
  `version` varchar(50) DEFAULT NULL COMMENT '版本号（本次验证对应的方案/镜像版本）',
  `executed_by` varchar(80) DEFAULT NULL COMMENT '执行人',
  `executed_date` date DEFAULT NULL COMMENT '执行日期',
  `result` varchar(20) NOT NULL DEFAULT 'passed' COMMENT '结果（passed 通过 / failed 未通过 / na 不适用）',
  `summary` text COMMENT '验证摘要',
  `file_name` varchar(255) DEFAULT NULL COMMENT '验证文档文件名（原文件名，附件存服务器固定目录）',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_validation_record_type` (`tenant_id`,`validation_type`),
  KEY `idx_validation_record_date` (`tenant_id`,`executed_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='3Q 验证记录';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `validation_record`
--

/*!40000 ALTER TABLE `validation_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `validation_record` ENABLE KEYS */;

--
-- Table structure for table `wet_lab_qc`
--

DROP TABLE IF EXISTS `wet_lab_qc`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `wet_lab_qc` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `subbarcode` varchar(80) NOT NULL COMMENT '样本条码（关联 cms_sample.subbarcode）',
  `patho_tumor_purity` varchar(50) DEFAULT NULL COMMENT '肿瘤纯度',
  `dna_total_ng` varchar(50) DEFAULT NULL COMMENT 'DNA总量（ng）',
  `dna_degradation` varchar(50) DEFAULT NULL COMMENT '检测结果判断（等级 A/B/C/D）',
  `pre_lib_total_ng` varchar(50) DEFAULT NULL COMMENT '预文库总量（ng）',
  `final_lib_conc` varchar(50) DEFAULT NULL COMMENT '终文库浓度（ng/uL）',
  `dna_total_ng_normal` varchar(50) DEFAULT NULL COMMENT 'DNA总量（ng）对照',
  `pre_lib_total_ng_normal` varchar(50) DEFAULT NULL COMMENT '预文库总量（ng）对照',
  `final_lib_conc_normal` varchar(50) DEFAULT NULL COMMENT '终文库浓度（ng/uL）对照',
  `status` varchar(20) NOT NULL DEFAULT 'pending' COMMENT '质控状态（passed 合格 / failed 不合格 / pending 待判定）',
  `notify_status` varchar(20) DEFAULT NULL COMMENT '通知发送状态（已生成（文档）/ 已发送（邮件））',
  `create_by` bigint(20) DEFAULT NULL COMMENT '创建者',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_by` bigint(20) DEFAULT NULL COMMENT '更新者',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
  `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_wet_lab_qc_subbarcode` (`tenant_id`,`subbarcode`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='湿实验质控（一行一个样本，列名=qc_param）';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wet_lab_qc`
--

/*!40000 ALTER TABLE `wet_lab_qc` DISABLE KEYS */;
/*!40000 ALTER TABLE `wet_lab_qc` ENABLE KEYS */;

--
-- Dumping routines for database 'bp_init_build'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-11-03 20:28:32
