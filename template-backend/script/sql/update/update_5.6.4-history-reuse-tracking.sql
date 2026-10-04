-- 位点匹配历史（history_somatic / history_germline）：冻结口径收紧（v2 复用键）+ 复用留痕
-- ---------------------------------------------------------------------------
-- 为什么：
--   需求：同一个「产品项目 + 位点 + 癌种 + 性别 + 人工改靶父级」下的解读结果（用药信息 / 位点等级）
--   必须永久一致 —— 即使 NKB 更新，相同 product 项目的解读也应是相同结果。
--   现有实现的两处会被覆盖：① 历史为旧结构时 refreshHistory 覆盖 match_result；
--                        ② 保存胚系临床意义时重跑匹配并覆盖 match_result / variation_class。
--   本脚本负责表结构：① 加 key_version（老键=v1 含 customer，新键=v2 按产品项目）
--                     ② 加 last_reused_at / reuse_count（每次复用登记时间）
--                     ③ 老口径行软删退役（del_flag='1'，不物理删，可回滚）
--                     ④ 新增 history_reuse_log（append-only 复用留痕）
-- 依据：docs/context/report/report-module-design.md §4.8；ai-rules/04-db-schema.md
-- 幂等：加列前查 information_schema；可反复执行
-- ---------------------------------------------------------------------------
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS add_history_reuse_columns;
DELIMITER $$
CREATE PROCEDURE add_history_reuse_columns()
BEGIN
    DECLARE done int DEFAULT 0;
    DECLARE t varchar(64);
    DECLARE tables_cursor CURSOR FOR
        SELECT TABLE_NAME FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME IN ('history_somatic', 'history_germline');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    OPEN tables_cursor;
    add_loop: LOOP
        FETCH tables_cursor INTO t;
        IF done = 1 THEN LEAVE add_loop; END IF;

        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t
                         AND COLUMN_NAME = 'key_version') THEN
            SET @s = CONCAT('ALTER TABLE `', t, '` ADD COLUMN key_version tinyint NOT NULL DEFAULT 1 '
                'COMMENT ''复用键口径版本：1=旧键(含customer)，2=按产品项目+位点+癌种+性别+改靶父级''');
            PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t
                         AND COLUMN_NAME = 'last_reused_at') THEN
            SET @s = CONCAT('ALTER TABLE `', t, '` ADD COLUMN last_reused_at datetime(3) DEFAULT NULL '
                'COMMENT ''最近一次被复用的时间''');
            PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
        END IF;

        IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t
                         AND COLUMN_NAME = 'reuse_count') THEN
            SET @s = CONCAT('ALTER TABLE `', t, '` ADD COLUMN reuse_count int NOT NULL DEFAULT 0 '
                'COMMENT ''被复用次数''');
            PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
        END IF;
    END LOOP;
    CLOSE tables_cursor;
END$$
DELIMITER ;

CALL add_history_reuse_columns();
DROP PROCEDURE IF EXISTS add_history_reuse_columns;

-- 退役 v1 行：只软删，不物理删（键口径已变，v1 键不可能再命中）
UPDATE history_somatic  SET del_flag = '1', update_time = NOW() WHERE del_flag = '0' AND key_version = 1;
UPDATE history_germline SET del_flag = '1', update_time = NOW() WHERE del_flag = '0' AND key_version = 1;

-- 复用留痕（append-only 日志类：按 ai-rules/04 §2 不加 del_flag）
CREATE TABLE IF NOT EXISTS `history_reuse_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
    `history_type` varchar(16) NOT NULL COMMENT 'SOMATIC / GERMLINE',
    `history_id` bigint NOT NULL COMMENT 'history_somatic / history_germline.id',
    `match_key` char(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '规范化匹配键',
    `gene` varchar(100) NOT NULL COMMENT '基因',
    `variant` varchar(500) NOT NULL COMMENT '规范化位点',
    `project_code` varchar(120) NOT NULL COMMENT 'analysis_data.product',
    `report_id` bigint NOT NULL COMMENT '复用时的报告ID',
    `analysis_id` bigint NOT NULL COMMENT '复用时的分析批次ID',
    `source_variant_id` bigint DEFAULT NULL COMMENT '复用时的来源位点ID',
    `reused_by` bigint DEFAULT NULL COMMENT '操作人 sys_user.user_id',
    `reused_at` datetime(3) NOT NULL COMMENT '复用时间',
    `create_by` bigint COMMENT '创建者',
    `create_time` datetime COMMENT '创建时间',
    `update_by` bigint COMMENT '更新者',
    `update_time` datetime COMMENT '更新时间',
    `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    KEY `idx_history_reuse_log_history` (`history_type`, `history_id`, `reused_at`),
    KEY `idx_history_reuse_log_report` (`report_id`, `reused_at`),
    KEY `idx_history_reuse_log_match_key` (`match_key`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='位点匹配历史复用留痕';
