-- 改靶支持「一个 / 多个父级」：把 parent_mutation_id 由 bigint 改为 varchar(255)，
-- 存逗号分隔的 NKB 节点ID（如 '2807,4877'，NULL/'' = 未改靶）。
-- 口径对齐 en7：ReportCrServiceImpl 读 parent_mutID 时 split(",") 后逐条追加进 mutationIdList。
-- 影响表：4 张位点明细表（改靶写入）+ 2 张匹配历史表（匹配键的一部分）。
-- 幂等：列已是 varchar 则跳过；可反复执行。
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS widen_parent_mutation_id;
DELIMITER $$
CREATE PROCEDURE widen_parent_mutation_id()
BEGIN
    DECLARE done int DEFAULT 0;
    DECLARE t varchar(64);
    DECLARE tables_cursor CURSOR FOR
        SELECT TABLE_NAME FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME IN ('file_Somatic_SNV_Indel', 'file_CNV', 'file_Fusion', 'file_CR_ALL',
                             'history_somatic', 'history_germline');
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
    OPEN tables_cursor;
    widen_loop: LOOP
        FETCH tables_cursor INTO t;
        IF done = 1 THEN LEAVE widen_loop; END IF;
        IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = t
                     AND COLUMN_NAME = 'parent_mutation_id' AND DATA_TYPE <> 'varchar') THEN
            SET @widen_sql = CONCAT('ALTER TABLE `', t, '` MODIFY COLUMN parent_mutation_id varchar(255) '
                'DEFAULT NULL COMMENT ''人工关联的NKB父级mutation ID（逗号分隔，可多个）''');
            PREPARE widen_statement FROM @widen_sql;
            EXECUTE widen_statement;
            DEALLOCATE PREPARE widen_statement;
        END IF;
    END LOOP;
    CLOSE tables_cursor;
END$$
DELIMITER ;

CALL widen_parent_mutation_id();
DROP PROCEDURE IF EXISTS widen_parent_mutation_id;
