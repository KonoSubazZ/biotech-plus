-- =====================================================================================
-- 项目配置：单表自关联，项目下可挂多个研究中心（每个中心一套收件/抄送邮箱）
--
-- 为什么（用户 2026-10-28）：管理中心按研究中心分别收报告，项目级一个邮箱不够用。
--   不建中心主表/关联表 —— 中心不跨项目复用，三张表是纯开销，且 xlsx 导入会跨表。
--   parent_id=0 → 项目行；parent_id=项目行 id → 研究中心行。
--   唯一键换成 uk_project_config_node(tenant_id, project_no, research_center_no, research_center_name)：
--     项目行两列空串 → 一项目一行；中心行 → 同项目内（编号,名称）不重复。
--   新增 4 列：contract_name / contract_code / study_plan_no（项目级展示字段，不参与业务逻辑）、
--             research_center_no（与 research_center_name 成对，中心行必填）。
-- 存量数据：全部是项目行，新列走默认值，无需回填；邮箱原样留在项目行。
-- 幂等：全部先查 information_schema 再动作，可重复执行。
-- =====================================================================================

USE `ry-vue`;
SET NAMES utf8mb4;

-- 1) parent_id
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND COLUMN_NAME = 'parent_id');
SET @s := IF(@c = 0,
  'ALTER TABLE `project_config` ADD COLUMN `parent_id` bigint NOT NULL DEFAULT 0 COMMENT ''父节点ID（0=项目行；研究中心行=所属项目行 id）'' AFTER `id`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2) 项目级展示字段 3 列（可空，仅项目行有值）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND COLUMN_NAME = 'contract_name');
SET @s := IF(@c = 0,
  'ALTER TABLE `project_config` ADD COLUMN `contract_name` varchar(200) DEFAULT NULL COMMENT ''合同名称（仅项目行有值；展示字段，不参与业务逻辑）'' AFTER `customer`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND COLUMN_NAME = 'contract_code');
SET @s := IF(@c = 0,
  'ALTER TABLE `project_config` ADD COLUMN `contract_code` varchar(100) DEFAULT NULL COMMENT ''合同编号（仅项目行有值；展示字段）'' AFTER `contract_name`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND COLUMN_NAME = 'study_plan_no');
SET @s := IF(@c = 0,
  'ALTER TABLE `project_config` ADD COLUMN `study_plan_no` varchar(100) DEFAULT NULL COMMENT ''研究方案编号（仅项目行有值；展示字段）'' AFTER `contract_code`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3) 研究中心编号 / 名称（NOT NULL DEFAULT ''，项目行为空串，参与唯一键）
SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND COLUMN_NAME = 'research_center_no');
SET @s := IF(@c = 0,
  'ALTER TABLE `project_config` ADD COLUMN `research_center_no` varchar(100) NOT NULL DEFAULT '''' COMMENT ''研究中心编号（仅中心行有值；项目行为空串）'' AFTER `study_plan_no`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @c := (SELECT COUNT(*) FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND COLUMN_NAME = 'research_center_name');
SET @s := IF(@c = 0,
  'ALTER TABLE `project_config` ADD COLUMN `research_center_name` varchar(200) NOT NULL DEFAULT '''' COMMENT ''研究中心名称（仅中心行有值；项目行为空串）'' AFTER `research_center_no`',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 4) 中心行不写 project_name/customer/邮箱 → 老列去掉「必须显式给值」的隐含约束
ALTER TABLE `project_config`
    MODIFY COLUMN `project_no`    varchar(60)  NOT NULL          COMMENT '项目编号（项目行唯一；中心行冗余存）',
    MODIFY COLUMN `project_name`  varchar(200) NOT NULL DEFAULT '' COMMENT '项目名称（仅项目行有值）',
    MODIFY COLUMN `customer`      varchar(200) NOT NULL DEFAULT '' COMMENT '客户名称（仅项目行有值）',
    MODIFY COLUMN `receive_email` varchar(500) NOT NULL DEFAULT '' COMMENT '收件邮箱（;分隔）；项目行=项目自身，中心行=该中心',
    COMMENT = '项目配置（项目行 + 研究中心行，parent_id 自关联）';

-- 5) 唯一键：uk_project_config_project_no → uk_project_config_node
SET @k := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND INDEX_NAME = 'uk_project_config_project_no');
SET @s := IF(@k > 0, 'ALTER TABLE `project_config` DROP INDEX `uk_project_config_project_no`', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @k := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND INDEX_NAME = 'uk_project_config_node');
SET @s := IF(@k = 0,
  'ALTER TABLE `project_config` ADD UNIQUE KEY `uk_project_config_node` (tenant_id, project_no, research_center_no, research_center_name)',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 6) 父节点索引（展开项目查中心、级联删除都走它）
SET @k := (SELECT COUNT(*) FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'project_config' AND INDEX_NAME = 'idx_project_config_parent');
SET @s := IF(@k = 0,
  'ALTER TABLE `project_config` ADD KEY `idx_project_config_parent` (tenant_id, parent_id)',
  'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 7) 兜底回填（幂等；正常情况 0 行）
UPDATE `project_config` SET `parent_id` = 0 WHERE `parent_id` IS NULL;
UPDATE `project_config` SET `research_center_no` = '' WHERE `research_center_no` IS NULL;
UPDATE `project_config` SET `research_center_name` = '' WHERE `research_center_name` IS NULL;
