-- =====================================================================================
-- 驱动入库（ruoyi-drive 模块）表结构迁移
--
-- 背景（用户 2026-10-21 口径）：
--   1. 「上传 / 重新解读」每次驱动都新建 analysis_data 记录（不同批次）；
--   2. 「重新驱动」在同一批次内重跑：老 data_file_status 行置 del_flag='1'，
--      再插入 del_flag='0' 的新行（新 file_id），保留历史；
--   3. 本期不写 tenant_id（后续要移除租户数据权限模块），唯一键不再带 tenant_id。
--
-- 原唯一键挡死了这两条口径，必须重建：
--   uk_analysis_data     (tenant_id, subbarcode, product, analysis_date)
--   uk_data_file_status  (tenant_id, file_path, file_name)
-- =====================================================================================

USE `ry-vue`;

-- -------------------------------------------------------------------------------------
-- 1. analysis_data：同一样本同一天允许存在多个批次
-- -------------------------------------------------------------------------------------
ALTER TABLE `analysis_data`
    ADD COLUMN `batch_no` INT NOT NULL DEFAULT 1 COMMENT '批次号：同一样本+产品+分析日期多次上传时递增' AFTER `analysis_date`;

ALTER TABLE `analysis_data` DROP INDEX `uk_analysis_data`;

ALTER TABLE `analysis_data`
    ADD UNIQUE KEY `uk_analysis_batch` (`analysis_date`, `product`, `subbarcode`, `batch_no`);

-- -------------------------------------------------------------------------------------
-- 2. data_file_status：同一文件允许多条历史行（老行 del_flag=1，新行 del_flag=0）
-- -------------------------------------------------------------------------------------
ALTER TABLE `data_file_status` DROP INDEX `uk_data_file_status`;

ALTER TABLE `data_file_status`
    ADD KEY `idx_file_path_name` (`file_path`, `file_name`);

-- -------------------------------------------------------------------------------------
-- 3. 权限点：四个驱动入口（外部生信流程调用 / 页面按钮共用）
--    挂在「报告解读」菜单下作为权限点（menu_type=F），前端不需要新页面。
--    superadmin 自动拥有全部权限，其他角色按需在「角色管理」里勾选。
-- -------------------------------------------------------------------------------------
SET @report_menu_id := (SELECT menu_id FROM `sys_menu` WHERE perms = 'report:interpretation:list' LIMIT 1);

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`, `remark`)
SELECT '驱动-按日期', @report_menu_id, 91, '', '', '', 1, 0, 'F', '0', '0', 'report:drive:date', '', 1, NOW(),
       '驱动某个日期下所有产品所有样本'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE perms = 'report:drive:date');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`, `remark`)
SELECT '驱动-按产品', @report_menu_id, 92, '', '', '', 1, 0, 'F', '0', '0', 'report:drive:product', '', 1, NOW(),
       '驱动某个日期某个产品下所有样本'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE perms = 'report:drive:product');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`, `remark`)
SELECT '驱动-按样本', @report_menu_id, 93, '', '', '', 1, 0, 'F', '0', '0', 'report:drive:sample', '', 1, NOW(),
       '驱动某个日期指定产品的某个样本'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE perms = 'report:drive:sample');

INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query_param`,
                        `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`,
                        `create_by`, `create_time`, `remark`)
SELECT '驱动-按文件', @report_menu_id, 94, '', '', '', 1, 0, 'F', '0', '0', 'report:drive:file', '', 1, NOW(),
       '重新驱动某个文件（同一批次，老行置删除）'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_menu` WHERE perms = 'report:drive:file');

-- -------------------------------------------------------------------------------------
-- 4. 已有角色继承：凡是已勾选「报告解读-文件」权限的角色，一并授予四个驱动权限点
--    （页面「重新驱动 / 重新解读」按钮直接改调新接口，不勾会让生信角色 403）
-- -------------------------------------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT DISTINCT rm.role_id, m.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` src ON src.menu_id = rm.menu_id AND src.perms = 'report:interpretation:files'
JOIN `sys_menu` m ON m.perms LIKE 'report:drive:%'
WHERE NOT EXISTS (
    SELECT 1 FROM `sys_role_menu` x WHERE x.role_id = rm.role_id AND x.menu_id = m.menu_id
);
