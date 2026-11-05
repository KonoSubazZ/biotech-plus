-- ----------------------------------------------------------------------------
-- 5.6.46：数据备份 → 样本备份（只读页面）
--
-- 只做三件事：建 backup_sample_file、加菜单「样本备份」、加权限点位并授权存量角色。
-- 不建 task 表：样本级汇总由页面按 sample_id 聚合。
-- 可重复执行（建表 IF NOT EXISTS，菜单/授权均带 NOT EXISTS 兜底）。
-- ----------------------------------------------------------------------------
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `backup_sample_file` (
  `id`              bigint(20)    NOT NULL AUTO_INCREMENT COMMENT '主键',
  `sample_id`       bigint(20)    NOT NULL COMMENT 'cms_sample.id',
  `report_id`       bigint(20)    DEFAULT NULL COMMENT 'analysis_report.report_id（一样本多报告时用于归属）',
  `subbarcode`      varchar(100)  NOT NULL COMMENT '样本编号',
  `project_no`      varchar(100)  DEFAULT NULL COMMENT '项目编号（cms_sample.project_no）',
  `order_code`      varchar(100)  DEFAULT NULL COMMENT '订单批次编号（cms_sample.order_code）',
  `product_bio`     varchar(120)  DEFAULT NULL COMMENT '生信产品编码（analysis_data.product_bio）',
  `research_center` varchar(200)  DEFAULT NULL COMMENT '研究中心/客户名称',
  `file_category`   varchar(32)   NOT NULL COMMENT 'report_docx/report_pdf/report_json/report_signed_pdf/notify_*/fastq/bam/vcf/other',
  `file_name`       varchar(255)  DEFAULT NULL COMMENT '文件名（含扩展名）',
  `server_host`     varchar(64)   NOT NULL DEFAULT '' COMMENT '文件所在主机（对应 sample.hosts 的键）',
  `server_path`     varchar(1024) NOT NULL COMMENT '宿主绝对路径（脚本直接读）',
  `path_hash`       char(32)      NOT NULL COMMENT 'md5(host:path)，幂等键',
  `file_size`       bigint(20)    DEFAULT NULL COMMENT '字节数',
  `file_md5`        varchar(32)   DEFAULT NULL COMMENT '源文件 MD5',
  `oss_path`        varchar(1024) DEFAULT NULL COMMENT 'OSS 对象 key',
  `oss_meta_md5`    varchar(32)   DEFAULT NULL COMMENT 'OSS x-oss-meta-file-md5 回读值',
  `backup_status`   tinyint(4)    NOT NULL DEFAULT 0 COMMENT '0待备份/1备份中/2成功/3失败/4跳过',
  `backup_time`     datetime      DEFAULT NULL COMMENT '备份完成时间',
  `fail_reason`     varchar(500)  DEFAULT NULL COMMENT '失败原因',
  `create_by`       bigint(20)    DEFAULT NULL COMMENT '创建者',
  `create_time`     datetime      DEFAULT NULL COMMENT '创建时间',
  `update_by`       bigint(20)    DEFAULT NULL COMMENT '更新者',
  `update_time`     datetime      DEFAULT NULL COMMENT '更新时间',
  `del_flag`        char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
  `tenant_id`       varchar(20)   NOT NULL DEFAULT '000000' COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bsf_sample_path` (`sample_id`, `path_hash`),
  KEY `idx_bsf_report` (`report_id`),
  KEY `idx_bsf_status` (`sample_id`, `backup_status`),
  KEY `idx_bsf_subbarcode` (`subbarcode`, `file_category`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='样本备份文件明细（一物理文件一行）';

-- 菜单：样本备份（挂到 route.backup 下，order 2）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
                        `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT 'route.backup_sample', p.menu_id, 2, 'sample', 'backup/sample/index', 1, 0,
       'C', '0', '0', 'backup:sample:list', 'carbon:data-base', 1, sysdate(), '样本备份'
FROM (SELECT menu_id FROM `sys_menu` WHERE `path` = 'backup' AND `menu_type` = 'M' LIMIT 1) p
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'backup:sample:list' LIMIT 1) x);

-- 按钮：查询
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`,
                        `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `remark`)
SELECT '查询', m.menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'backup:sample:query', '#', 1, sysdate(), ''
FROM (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'backup:sample:list' LIMIT 1) m
WHERE NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu` WHERE `perms` = 'backup:sample:query' LIMIT 1) x);

-- 授权：已授过「数据备份」父菜单的角色，自动补上样本备份的两个点位
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, m.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON m.`perms` IN ('backup:sample:list', 'backup:sample:query')
WHERE rm.menu_id = (SELECT menu_id FROM (SELECT menu_id FROM `sys_menu` WHERE `path` = 'backup' AND `menu_type` = 'M' LIMIT 1) t)
  AND NOT EXISTS (SELECT 1 FROM (SELECT role_id, menu_id FROM `sys_role_menu`) z
                  WHERE z.role_id = rm.role_id AND z.menu_id = m.menu_id);
