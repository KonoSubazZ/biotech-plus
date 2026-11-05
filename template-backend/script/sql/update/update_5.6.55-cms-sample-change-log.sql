-- ============================================================================
-- cms_sample_change_log：样本信息变更流水（抽屉「历史修改信息」的数据源）
--
-- 与 cms_sample.sync_log 的区别：
--   sync_log      = 最近一次刷新的变更明细，覆盖写（只留最后一次）
--   本表          = 追加写，刷新 / 编辑每次一条变更记录一行，用于历史追溯
--
-- 数据来源（2026-11-04 用户口径）：
--   1) 行内「从CMS更新样本信息」刷新 —— source = refresh
--   2) 抽屉内「编辑」保存         —— source = edit
--   定时任务（SYSTEM）与 Excel 导入不在本次口径内，不写本表。
--
-- 幂等：CREATE TABLE IF NOT EXISTS，可反复跑。
-- 执行：docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
--         mysql -uroot --default-character-set=utf8mb4 biotech < update_5.6.55-cms-sample-change-log.sql
-- ============================================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cms_sample_change_log` (
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
