-- 样本流转（报告管理 › 样本流转）：状态机改数字码 + 报告人员字段统一 + 菜单权限
-- ============================================================================
-- 背景：原来的报告只有 5 个字符串状态（INTERPRETING/PENDING_REVIEW/APPROVED/REJECTED/SENT），
--       无法表达「驱动 → 生信看点 → 生信审核 → 待解读 → 解读中 → 生成 → 待审核 → 审核中 →
--       签名 → 发送」这条完整流转，也承载不了每个环节的处理人与失败原因。
-- 本次口径（用户 2026-10-16 逐条确认，见 skill：module-design-and-test-doc-delivery 的
--       references/biotech-plus-business-flow.md §8）：
--       1. 两个状态列都改 int，用数字码；老值先按映射改值再改类型。
--          analysis_data.drive_status：10 驱动失败 / 11 驱动成功 / 12 驱动中
--          analysis_report.report_status（原 status 改名）：20 生信看点 / 21 生信审核 /
--            30 报告待解读 / 31 报告解读中 / 32 报告生成成功 / 33 报告生成失败 /
--            40 报告待审核 / 41 报告审核中 / 42 报告审核失败 / 43 报告审核成功 /
--            50 报告签名中 / 52 报告签名失败 / 53 报告签名成功 / 60 报告待发送 /
--            61 报告发送失败 / 62 报告发送成功（终态＝完成报告）
--       2. 报告环节的处理人字段统一：report_<环节>_by（bigint，存 sys_user.user_id）+ report_<环节>_at；
--          既有的 report_generated_by / report_checked_by / report_sent_by 由 varchar 改 bigint（同一口径）。
--       3. 签名字段统一 report_ 前缀：sign_flow_no → report_sign_flow_no、sign_time → report_sign_time；
--          sign_status 退役（状态合并进 report_status 的 50/52/53）。
--       4. 病理师固定账号：bingli / 13333335555 / 昵称「测试病理师」（送签三方之一，需 username + phone）。
--       5. 菜单：报告管理下新增「样本流转」（report:flow:list）+ 10 个页签/动作权限点。
-- 幂等：值映射按「列还是字符串类型」判断（改过就跳过），列增删改先查 information_schema，
--       菜单/账号按 NOT EXISTS 插入 —— 整份脚本可以重复执行。
-- ============================================================================
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 0. 幂等辅助过程
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_flow_add_column;
DROP PROCEDURE IF EXISTS p_flow_rename_column;
DROP PROCEDURE IF EXISTS p_flow_drop_column;
DROP PROCEDURE IF EXISTS p_flow_migrate_drive_status;
DROP PROCEDURE IF EXISTS p_flow_migrate_report_status;

DELIMITER $$
CREATE PROCEDURE p_flow_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl VARCHAR(1000))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE p_flow_rename_column(IN p_table VARCHAR(64), IN p_old VARCHAR(64), IN p_new VARCHAR(64), IN p_ddl VARCHAR(1000))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_old)
       AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_new) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` CHANGE COLUMN `', p_old, '` ', p_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

CREATE PROCEDURE p_flow_drop_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

-- 驱动状态：仅当还是字符串列时做老值映射（已经迁移过就跳过）
CREATE PROCEDURE p_flow_migrate_drive_status()
BEGIN
    DECLARE v_type VARCHAR(64);
    SELECT DATA_TYPE INTO v_type FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_data' AND COLUMN_NAME = 'drive_status' LIMIT 1;
    IF v_type IS NOT NULL AND v_type <> 'int' THEN
        UPDATE analysis_data SET drive_status = '12' WHERE drive_status = 'DRIVING';
        UPDATE analysis_data SET drive_status = '11' WHERE drive_status = 'LOADED';
        UPDATE analysis_data SET drive_status = '10' WHERE drive_status IN ('PARTIAL', 'FAILED');
    END IF;
END$$

-- 报告状态：仅当 status 列还在且还是字符串类型时做老值映射 + 改名 + 改 int
CREATE PROCEDURE p_flow_migrate_report_status()
BEGIN
    DECLARE v_type VARCHAR(64);
    SELECT DATA_TYPE INTO v_type FROM information_schema.COLUMNS
     WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_report' AND COLUMN_NAME = 'status' LIMIT 1;
    IF v_type IS NOT NULL AND v_type <> 'int' THEN
        UPDATE analysis_report SET status = '31' WHERE status = 'INTERPRETING';
        UPDATE analysis_report SET status = '40' WHERE status = 'PENDING_REVIEW';
        UPDATE analysis_report SET status = '43' WHERE status = 'APPROVED';
        UPDATE analysis_report SET status = '42' WHERE status = 'REJECTED';
        UPDATE analysis_report SET status = '62' WHERE status = 'SENT';
        -- 其它未识别值落到起始态，避免改 int 时丢成 0
        UPDATE analysis_report SET status = '20'
         WHERE status NOT IN ('20','21','30','31','32','33','40','41','42','43','50','52','53','60','61','62');
        ALTER TABLE analysis_report CHANGE COLUMN status report_status int NOT NULL DEFAULT 20
            COMMENT '报告状态（20 生信看点 / 21 生信审核 / 30 报告待解读 / 31 报告解读中 / 32 报告生成成功 / 33 报告生成失败 / 40 报告待审核 / 41 报告审核中 / 42 报告审核失败 / 43 报告审核成功 / 50 报告签名中 / 52 报告签名失败 / 53 报告签名成功 / 60 报告待发送 / 61 报告发送失败 / 62 报告发送成功）';
    END IF;
END$$
DELIMITER ;

-- ---------------------------------------------------------------------------
-- 1. analysis_data.drive_status：老值映射 → 改 int
-- ---------------------------------------------------------------------------
CALL p_flow_migrate_drive_status();

-- 可重复执行：已经是 int 时等价于只刷新列注释
ALTER TABLE analysis_data
    MODIFY COLUMN drive_status int NULL DEFAULT NULL COMMENT '驱动状态（10 驱动失败 / 11 驱动成功 / 12 驱动中）';

-- ---------------------------------------------------------------------------
-- 2. analysis_report.status：老值映射 → 改名 report_status → 改 int
-- ---------------------------------------------------------------------------
CALL p_flow_migrate_report_status();

-- 兜底：列还是老名字（status）时按老容器改名（正常已在上面完成）
CALL p_flow_rename_column('analysis_report', 'status', 'report_status',
    'report_status int NOT NULL DEFAULT 20 COMMENT ''报告状态（20 生信看点 / 21 生信审核 / 30 报告待解读 / 31 报告解读中 / 32 报告生成成功 / 33 报告生成失败 / 40 报告待审核 / 41 报告审核中 / 42 报告审核失败 / 43 报告审核成功 / 50 报告签名中 / 52 报告签名失败 / 53 报告签名成功 / 60 报告待发送 / 61 报告发送失败 / 62 报告发送成功）''');

-- 可重复执行：保证类型/注释就是这份定义
ALTER TABLE analysis_report
    MODIFY COLUMN report_status int NOT NULL DEFAULT 20 COMMENT '报告状态（20 生信看点 / 21 生信审核 / 30 报告待解读 / 31 报告解读中 / 32 报告生成成功 / 33 报告生成失败 / 40 报告待审核 / 41 报告审核中 / 42 报告审核失败 / 43 报告审核成功 / 50 报告签名中 / 52 报告签名失败 / 53 报告签名成功 / 60 报告待发送 / 61 报告发送失败 / 62 报告发送成功）';

-- ---------------------------------------------------------------------------
-- 3. analysis_report 人员/流程字段：统一 report_<环节>_by + _at（存 user_id）
-- ---------------------------------------------------------------------------
CALL p_flow_add_column('analysis_report', 'report_bioinfo_viewed_by',
    'report_bioinfo_viewed_by bigint NULL COMMENT ''生信看点人（sys_user.user_id）''');
CALL p_flow_add_column('analysis_report', 'report_bioinfo_viewed_at',
    'report_bioinfo_viewed_at datetime NULL COMMENT ''生信看点完成时间''');
CALL p_flow_add_column('analysis_report', 'report_bioinfo_reviewed_by',
    'report_bioinfo_reviewed_by bigint NULL COMMENT ''生信复核人（sys_user.user_id）''');
CALL p_flow_add_column('analysis_report', 'report_bioinfo_reviewed_at',
    'report_bioinfo_reviewed_at datetime NULL COMMENT ''生信复核时间''');
CALL p_flow_add_column('analysis_report', 'report_interpreted_by',
    'report_interpreted_by bigint NULL COMMENT ''报告解读人（认领人 sys_user.user_id）''');
CALL p_flow_add_column('analysis_report', 'report_interpreted_at',
    'report_interpreted_at datetime NULL COMMENT ''报告解读认领时间''');
CALL p_flow_add_column('analysis_report', 'report_review_claimed_by',
    'report_review_claimed_by bigint NULL COMMENT ''报告复核认领人（sys_user.user_id）''');
CALL p_flow_add_column('analysis_report', 'report_review_claimed_at',
    'report_review_claimed_at datetime NULL COMMENT ''报告复核认领时间''');
CALL p_flow_add_column('analysis_report', 'report_pathologist_by',
    'report_pathologist_by bigint NULL COMMENT ''病理师（固定账号 bingli，sys_user.user_id）''');
CALL p_flow_add_column('analysis_report', 'report_revert_from',
    'report_revert_from tinyint NULL COMMENT ''回退来源状态（审核失败回退时记原状态）''');
CALL p_flow_add_column('analysis_report', 'report_revert_reason',
    'report_revert_reason varchar(300) NULL COMMENT ''回退原因（审核失败）''');
CALL p_flow_add_column('analysis_report', 'report_gen_fail_reason',
    'report_gen_fail_reason varchar(500) NULL COMMENT ''报告生成失败原因（33）''');
CALL p_flow_add_column('analysis_report', 'report_sign_fail_reason',
    'report_sign_fail_reason varchar(300) NULL COMMENT ''报告签名失败原因（52）''');

-- 既有三个署名列由 varchar 改 bigint（值本来就是 user_id，如 '1'/'27'）
ALTER TABLE analysis_report
    MODIFY COLUMN report_generated_by bigint NULL COMMENT '报告生成人（sys_user.user_id）',
    MODIFY COLUMN report_checked_by   bigint NULL COMMENT '报告复核人（sys_user.user_id）',
    MODIFY COLUMN report_sent_by      bigint NULL COMMENT '报告发送人（sys_user.user_id）';

-- ---------------------------------------------------------------------------
-- 4. 签名字段统一前缀；sign_status 退役（合并进 report_status 的 50/52/53）
-- ---------------------------------------------------------------------------
CALL p_flow_rename_column('analysis_report', 'sign_flow_no', 'report_sign_flow_no',
    'report_sign_flow_no varchar(80) NULL COMMENT ''上上签合同/流程号''');
CALL p_flow_rename_column('analysis_report', 'sign_time', 'report_sign_time',
    'report_sign_time datetime NULL COMMENT ''签名完成时间（上上签回调）''');
CALL p_flow_drop_column('analysis_report', 'sign_status');

-- ---------------------------------------------------------------------------
-- 5. 病理师固定账号（送签三方之一：需要 username + phone）
--    密码与平台测试账号 test 相同（666666），首次登录后可自行修改
-- ---------------------------------------------------------------------------
INSERT INTO sys_user (tenant_id, user_name, nick_name, user_type, email, phonenumber,
                      sex, avatar, password, status, del_flag, login_ip, login_date,
                      create_by, create_time, update_by, update_time, remark)
SELECT '000000', 'bingli', '测试病理师', 'sys_user', NULL, '13333335555',
        '0', NULL, u.password, '0', '0', NULL, NULL,
        1, NOW(), NULL, NULL, '报告签名固定病理师（样本流转/报告签名送签三方之一）'
FROM sys_user u
WHERE u.user_name = 'test'
  AND NOT EXISTS (SELECT 1 FROM sys_user x WHERE x.user_name = 'bingli');

-- ---------------------------------------------------------------------------
-- 6. 菜单与权限点：报告管理 › 样本流转（列表 + 10 个页签/动作权限点）
-- ---------------------------------------------------------------------------
SET @report_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'report' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '样本流转', @report_menu_id, 6, 'sample-flow', 'report/sample-flow/index', 1, 0,
       'C', '0', '0', 'report:flow:list', 'local-icon-build', 1, NOW(),
       '报告管理 › 样本流转（9 个页签的流转视图）'
FROM DUAL
WHERE @report_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = @report_menu_id AND x.path = 'sample-flow');

SET @flow_menu_id = (SELECT menu_id FROM sys_menu WHERE parent_id = @report_menu_id AND path = 'sample-flow' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                      menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT t.menu_name, @flow_menu_id, t.order_num, '', NULL, 1, 0, 'F', '0', '0', t.perms, '#', 1, NOW(), t.remark
FROM (SELECT '处理' AS menu_name, 1 AS order_num, 'report:flow:process' AS perms,
             '页签操作列「处理」（跳转报告解读 / 更新状态）' AS remark
      UNION ALL SELECT '驱动异常', 2, 'report:flow:driver', '页签1.1 驱动异常（drive_status=10）'
      UNION ALL SELECT '生信看点', 3, 'report:flow:bioinfo-view', '页签1.2 生信看点（11→20）'
      UNION ALL SELECT '生信审核', 4, 'report:flow:bioinfo-review', '页签1.3 生信审核（21）'
      UNION ALL SELECT '报告待解读', 5, 'report:flow:pending-interpret', '页签1.4 报告待解读（30）'
      UNION ALL SELECT '报告解读中', 6, 'report:flow:interpreting', '页签1.5 报告解读中（31/32/33）'
      UNION ALL SELECT '报告待审核', 7, 'report:flow:pending-review', '页签1.6 报告待审核（40）'
      UNION ALL SELECT '报告审核中', 8, 'report:flow:reviewing', '页签1.7 报告审核中（41）'
      UNION ALL SELECT '报告签名', 9, 'report:flow:signing', '页签1.8 报告签名（50/52/53）'
      UNION ALL SELECT '报告发送', 10, 'report:flow:sending', '页签1.9 报告发送（60/61/62）') AS t
WHERE @flow_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = @flow_menu_id AND x.perms = t.perms);

-- ---------------------------------------------------------------------------
-- 7. 清理辅助过程
-- ---------------------------------------------------------------------------
DROP PROCEDURE IF EXISTS p_flow_add_column;
DROP PROCEDURE IF EXISTS p_flow_rename_column;
DROP PROCEDURE IF EXISTS p_flow_drop_column;
DROP PROCEDURE IF EXISTS p_flow_migrate_drive_status;
DROP PROCEDURE IF EXISTS p_flow_migrate_report_status;

-- ---------------------------------------------------------------------------
-- 8. 复核（人工执行）
-- ---------------------------------------------------------------------------
-- SELECT analysis_id, subbarcode, drive_status FROM analysis_data ORDER BY analysis_id;
-- SELECT report_id, analysis_id, subbarcode, report_status, report_interpreted_by, report_pathologist_by FROM analysis_report ORDER BY report_id;
-- SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_COMMENT FROM information_schema.COLUMNS
--   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'analysis_report' ORDER BY ORDINAL_POSITION;
-- SELECT m.menu_id, m.menu_name, m.perms, m.order_num FROM sys_menu m
--   WHERE m.menu_id = @flow_menu_id OR m.parent_id = @flow_menu_id ORDER BY m.order_num;
-- SELECT user_id, tenant_id, user_name, nick_name, phonenumber FROM sys_user WHERE user_name = 'bingli';
