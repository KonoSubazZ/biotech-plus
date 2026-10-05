-- ============================================================================
-- 报告管理 › 历史检出：位点检出历史表 + 菜单/权限
-- 用途：提交审核时把该报告「报出」的位点落库（ReportVariantHistoryWriter），
--       「历史检出」页按 报告时间范围/样本编号/解读癌种/产品/客户 检索。
-- 幂等：表用 CREATE TABLE IF NOT EXISTS；菜单按 path/permission 判 NOT EXISTS，可重复执行。
-- ============================================================================

CREATE TABLE IF NOT EXISTS `report_variant_history` (
    `id`                  bigint        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `report_id`           bigint        NOT NULL COMMENT '报告ID（analysis_report.report_id）',
    `analysis_id`         bigint        NOT NULL COMMENT '分析批次ID（analysis_report.analysis_id）',
    `subbarcode`          varchar(120)  DEFAULT NULL COMMENT '样本编号（analysis_report.subbarcode）',
    `client`              varchar(255)  DEFAULT NULL COMMENT '客户（LIMS 医院名；缺失时为空）',
    `product`             varchar(255)  DEFAULT NULL COMMENT '产品（analysis_report.product）',
    `disease`             varchar(255)  DEFAULT NULL COMMENT '解读癌种（本次提交审核生效的癌种）',
    `source_type`         varchar(32)   NOT NULL COMMENT '来源：SNP_INDEL/CNV/FUSION/CR_ALL',
    `source_id`           bigint        NOT NULL COMMENT '源位点明细表主键（file_*.id）',
    `gene`                varchar(100)  NOT NULL COMMENT '基因',
    `variant`             varchar(500)  NOT NULL COMMENT '规范化位点（NKB gene_variant）',
    `abundance`           varchar(64)   DEFAULT NULL COMMENT '丰度：体细胞/CNV/融合 = 预览页「丰度/reads」列（DNA 百分比 / RNA reads / 拷贝数）；胚系 = 合子状态 zygosity（het/hom，同 history_germline.measurement）',
    `variant_type`        varchar(120)  DEFAULT NULL COMMENT '突变类型（VEP ExonicFunc 中文：错义突变/移码突变…，= 预览页「变异类型」列 / PreviewVariantVo.variantTypeText）',
    `ori_variant`         varchar(1000) DEFAULT NULL COMMENT '原始位点描述',
    `level`               varchar(64)   DEFAULT NULL COMMENT '位点等级：体细胞 I/II/III 类；胚系为临床意义标签',
    `drug_info`           json          DEFAULT NULL COMMENT '原始用药信息（en7 drugGroups 原样：drugsA/drugsB/drugsC/drugsD/resistantDrugsA~D，值以分号分隔、需其他检测的药名带 #）',
    `analyzer`            varchar(64)   DEFAULT NULL COMMENT '解读人（报告生成参数里的解读人）',
    `report_generated_at` datetime      DEFAULT NULL COMMENT '报告时间（analysis_report.report_generated_at）',
    `create_by`           bigint        DEFAULT NULL COMMENT '创建者',
    `create_time`         datetime      DEFAULT NULL COMMENT '创建时间',
    `update_by`           bigint        DEFAULT NULL COMMENT '更新者',
    `update_time`         datetime      DEFAULT NULL COMMENT '更新时间',
    `del_flag`            char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`           varchar(20)   NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_variant_history_row` (`tenant_id`, `report_id`, `source_type`, `source_id`),
    KEY `idx_report_variant_history_query` (`report_generated_at`, `subbarcode`, `del_flag`),
    KEY `idx_report_variant_history_gene`  (`gene`, `variant`(120), `del_flag`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci ROW_FORMAT=DYNAMIC COMMENT='报告位点检出历史（提交审核时落库）';


-- ============================================================================
-- 已有库加「丰度」列（口径 = 预览页「丰度/reads」列 = PreviewVariantVo.abundanceText）
-- MySQL 5.7 没有 ADD COLUMN IF NOT EXISTS，用 information_schema 判断后动态执行，可重复跑
-- ============================================================================
SET @has_abundance = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'report_variant_history' AND column_name = 'abundance');
SET @ddl = IF(@has_abundance = 0,
    'ALTER TABLE `report_variant_history` ADD COLUMN `abundance` varchar(64) DEFAULT NULL COMMENT ''丰度（= 预览页「丰度/reads」列：DNA 百分比 / RNA reads / 扩增缺失拷贝数；无口径为空）'' AFTER `variant`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 列注释同步（口径补充：胚系取 zygosity）。注释里没有 zygosity 才改，可重复跑
SET @abundance_comment = (SELECT column_comment FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'report_variant_history' AND column_name = 'abundance');
SET @ddl_comment = IF(@abundance_comment NOT LIKE '%zygosity%',
    'ALTER TABLE `report_variant_history` MODIFY COLUMN `abundance` varchar(64) DEFAULT NULL COMMENT ''丰度：体细胞/CNV/融合 = 预览页「丰度/reads」列（DNA 百分比 / RNA reads / 拷贝数）；胚系 = 合子状态 zygosity（het/hom，同 history_germline.measurement）''',
    'SELECT 1');
PREPARE stmt_cmt FROM @ddl_comment;
EXECUTE stmt_cmt;
DEALLOCATE PREPARE stmt_cmt;

-- 已有库加「突变类型」列（VEP ExonicFunc 中文；位点类型直接用已有的 source_type 列，不重复建）
SET @has_variant_type = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'report_variant_history' AND column_name = 'variant_type');
SET @ddl_vt = IF(@has_variant_type = 0,
    'ALTER TABLE `report_variant_history` ADD COLUMN `variant_type` varchar(120) DEFAULT NULL COMMENT ''突变类型（VEP ExonicFunc 中文：错义突变/移码突变…，= 预览页「变异类型」列）'' AFTER `abundance`',
    'SELECT 1');
PREPARE stmt_vt FROM @ddl_vt;
EXECUTE stmt_vt;
DEALLOCATE PREPARE stmt_vt;


-- ============================================================================
-- 菜单：「报告管理」（父，path='report'）›「历史检出」
-- 幂等：按 path 定位父菜单（不写死 menu_id）；NOT EXISTS 包派生表（MySQL 5.7 无 LATERAL）
-- ============================================================================
SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.report_variant-history', @report_menu_id, 4, 'variant-history', 'report/variant-history/index',
    1, 0, 'C', '0', '0', 'report:variantHistory:list', 'local-icon-search', 1, NOW(), '历史检出'
FROM (SELECT 1) AS dummy
WHERE @report_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @report_menu_id AND path = 'variant-history') AS c0);

SET @history_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @report_menu_id AND path = 'variant-history' AND menu_type = 'C' LIMIT 1);

-- 按钮权限（F）：与 @SaCheckPermission 逐字一致，不进侧边栏
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @history_menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'report:variantHistory:list', '#', 1, NOW(), '查询检出历史'
FROM (SELECT 1) AS dummy
WHERE @history_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = @history_menu_id
      AND x.perms = 'report:variantHistory:list');

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '导出', @history_menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'report:variantHistory:export', '#', 1, NOW(), '导出 xlsx'
FROM (SELECT 1) AS dummy
WHERE @history_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = @history_menu_id
      AND x.perms = 'report:variantHistory:export');

-- 授权给超级管理员角色
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.menu_id = @history_menu_id OR m.parent_id = @history_menu_id;
