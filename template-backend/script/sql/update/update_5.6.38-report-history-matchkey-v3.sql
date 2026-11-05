-- =====================================================================================
-- 历史位点匹配键升到 v3：项目编号 + 产品项目 + 癌种 + 性别 + 位点
--
-- 用户口径（2026-10-22）：
--   匹配到历史位点必须 gene + variant + disease + gender + project_no + product_bio 全一致
--   （product_bio 即原 project_code 列的值）。v2 只有产品项目，跨项目会误命中，故升 v3。
--
-- 新键：sha256("v3|<project_no>|<project_code(product_bio)>|<disease: id|name>|<gender>|<gene>|<variant>|<oriVariant>|<parentIds>")
-- 代码侧：VariantMatcher.MATCH_KEY_VERSION = "v3"；胚系意义继承 where 同步加 project_no。
-- legacy:<id> 行（5.6.37 归一撞键时保留的审计行）不重算，保持不可命中。
-- =====================================================================================

USE `ry-vue`;

UPDATE `history_somatic`
SET `match_key` = SHA2(CONCAT(
        'v3', '|',
        TRIM(IFNULL(`project_no`, '')), '|',
        TRIM(IFNULL(`project_code`, '')), '|',
        IF(`disease_id` IS NULL, CONCAT('name:', TRIM(IFNULL(`disease`, ''))), CONCAT('id:', `disease_id`)), '|',
        TRIM(IFNULL(`gender`, '')), '|',
        TRIM(`gene`), '|',
        TRIM(`variant`), '|',
        TRIM(IFNULL(`ori_variant`, '')), '|',
        TRIM(IFNULL(`parent_mutation_id`, ''))), 256),
    `key_version` = 3
WHERE `key_version` = 2
  AND `match_key` NOT LIKE 'legacy:%';

UPDATE `history_germline`
SET `match_key` = SHA2(CONCAT(
        'v3', '|',
        TRIM(IFNULL(`project_no`, '')), '|',
        TRIM(IFNULL(`project_code`, '')), '|',
        IF(`disease_id` IS NULL, CONCAT('name:', TRIM(IFNULL(`disease`, ''))), CONCAT('id:', `disease_id`)), '|',
        TRIM(IFNULL(`gender`, '')), '|',
        TRIM(`gene`), '|',
        TRIM(`variant`), '|',
        TRIM(IFNULL(`ori_variant`, '')), '|',
        TRIM(IFNULL(`parent_mutation_id`, ''))), 256),
    `key_version` = 3
WHERE `key_version` = 2
  AND `match_key` NOT LIKE 'legacy:%';

-- 核对：版本分布 + 撞键（应为 0）
SELECT 'history_somatic' AS t, `key_version`, COUNT(*) AS n FROM `history_somatic` GROUP BY `key_version`
UNION ALL
SELECT 'history_germline', `key_version`, COUNT(*) FROM `history_germline` GROUP BY `key_version`;

SELECT 'somatic_dup' AS t, COUNT(*) AS n FROM (
    SELECT `match_key` FROM `history_somatic`
    WHERE `key_version` = 3 GROUP BY `match_key` HAVING COUNT(*) > 1) x
UNION ALL
SELECT 'germline_dup', COUNT(*) FROM (
    SELECT `match_key` FROM `history_germline`
    WHERE `key_version` = 3 GROUP BY `match_key` HAVING COUNT(*) > 1) y;
