-- =====================================================================================
-- 历史位点匹配键升到 v4：把「改靶父级」从键里拿掉
--
-- 用户口径（2026-10-22，第二轮）：
--   同一项目 + 同一生信产品 + 癌种 + 性别 + 基因 + 位点 + 原始位点 都相同，就应当命中历史。
--   改靶父级不能进键：历史是人为改过靶（加父级）的，实时查知识库算出来的父级与历史不一致时，
--   反而匹配不上历史 —— 而这正是要复用历史（人工确认过）的场景。
--
-- 新键：sha256("v4|<project_no>|<project_code=product_bio>|<disease: id|name>|<gender>|<gene>|<variant>|<oriVariant>")
-- 改靶父级仍会写进 history_* 的 parent_mutation_id 列（审计/查看用），也仍参与"首次实时匹配"的用药计算。
--
-- 撞键处理：去掉父级后 somatic 有 1 组撞键（KIT/V559D：id 28 父级=4877,5257,6536 复用 55 次；
-- id 74 无父级、从未复用）。保留顺序：父级非空 > reuse_count 大 > id 大 ⇒ 保留 28，74 迁 legacy:74。
-- =====================================================================================

USE `ry-vue`;

-- -------------------------------------------------------------------------------------
-- 1. history_somatic：先处理撞键，再重算 v4
-- -------------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_dup_somatic_v4;
CREATE TEMPORARY TABLE tmp_dup_somatic_v4 AS
SELECT k,
       CAST(SUBSTRING_INDEX(GROUP_CONCAT(id ORDER BY (parent_mutation_id IS NULL OR parent_mutation_id = '') ASC,
                                                        reuse_count DESC, id DESC), ',', 1) AS UNSIGNED) AS keeper
FROM (
    SELECT id, parent_mutation_id, reuse_count, SHA2(CONCAT(
               'v4', '|', TRIM(IFNULL(project_no, '')), '|', TRIM(IFNULL(project_code, '')), '|',
               IF(disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(disease, ''))), CONCAT('id:', disease_id)), '|',
               TRIM(IFNULL(gender, '')), '|', TRIM(gene), '|', TRIM(variant), '|',
               TRIM(IFNULL(ori_variant, ''))), 256) AS k
    FROM history_somatic
    WHERE key_version = 3
) t
GROUP BY k
HAVING COUNT(*) > 1;

UPDATE history_somatic h
    JOIN tmp_dup_somatic_v4 d
      ON d.k = SHA2(CONCAT(
             'v4', '|', TRIM(IFNULL(h.project_no, '')), '|', TRIM(IFNULL(h.project_code, '')), '|',
             IF(h.disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(h.disease, ''))), CONCAT('id:', h.disease_id)), '|',
             TRIM(IFNULL(h.gender, '')), '|', TRIM(h.gene), '|', TRIM(h.variant), '|',
             TRIM(IFNULL(h.ori_variant, ''))), 256)
     AND h.id <> d.keeper
SET h.match_key = CONCAT('legacy:', h.id)
WHERE h.key_version = 3;

UPDATE history_somatic
SET match_key = SHA2(CONCAT(
        'v4', '|', TRIM(IFNULL(project_no, '')), '|', TRIM(IFNULL(project_code, '')), '|',
        IF(disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(disease, ''))), CONCAT('id:', disease_id)), '|',
        TRIM(IFNULL(gender, '')), '|', TRIM(gene), '|', TRIM(variant), '|',
        TRIM(IFNULL(ori_variant, ''))), 256),
    key_version = 4
WHERE key_version = 3
  AND match_key NOT LIKE 'legacy:%';

-- -------------------------------------------------------------------------------------
-- 2. history_germline：暂无撞键，直接重算
-- -------------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_dup_germline_v4;
CREATE TEMPORARY TABLE tmp_dup_germline_v4 AS
SELECT k,
       CAST(SUBSTRING_INDEX(GROUP_CONCAT(id ORDER BY (parent_mutation_id IS NULL OR parent_mutation_id = '') ASC,
                                                        reuse_count DESC, id DESC), ',', 1) AS UNSIGNED) AS keeper
FROM (
    SELECT id, parent_mutation_id, reuse_count, SHA2(CONCAT(
               'v4', '|', TRIM(IFNULL(project_no, '')), '|', TRIM(IFNULL(project_code, '')), '|',
               IF(disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(disease, ''))), CONCAT('id:', disease_id)), '|',
               TRIM(IFNULL(gender, '')), '|', TRIM(gene), '|', TRIM(variant), '|',
               TRIM(IFNULL(ori_variant, ''))), 256) AS k
    FROM history_germline
    WHERE key_version = 3
) t
GROUP BY k
HAVING COUNT(*) > 1;

UPDATE history_germline h
    JOIN tmp_dup_germline_v4 d
      ON d.k = SHA2(CONCAT(
             'v4', '|', TRIM(IFNULL(h.project_no, '')), '|', TRIM(IFNULL(h.project_code, '')), '|',
             IF(h.disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(h.disease, ''))), CONCAT('id:', h.disease_id)), '|',
             TRIM(IFNULL(h.gender, '')), '|', TRIM(h.gene), '|', TRIM(h.variant), '|',
             TRIM(IFNULL(h.ori_variant, ''))), 256)
     AND h.id <> d.keeper
SET h.match_key = CONCAT('legacy:', h.id)
WHERE h.key_version = 3;

UPDATE history_germline
SET match_key = SHA2(CONCAT(
        'v4', '|', TRIM(IFNULL(project_no, '')), '|', TRIM(IFNULL(project_code, '')), '|',
        IF(disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(disease, ''))), CONCAT('id:', disease_id)), '|',
        TRIM(IFNULL(gender, '')), '|', TRIM(gene), '|', TRIM(variant), '|',
        TRIM(IFNULL(ori_variant, ''))), 256),
    key_version = 4
WHERE key_version = 3
  AND match_key NOT LIKE 'legacy:%';

-- -------------------------------------------------------------------------------------
-- 3. 核对
-- -------------------------------------------------------------------------------------
SELECT 'history_somatic' AS t, key_version, COUNT(*) AS n FROM history_somatic GROUP BY key_version
UNION ALL SELECT 'history_germline', key_version, COUNT(*) FROM history_germline GROUP BY key_version;

SELECT 'somatic_dup' AS t, COUNT(*) AS n FROM (
    SELECT match_key FROM history_somatic WHERE key_version = 4
    GROUP BY match_key HAVING COUNT(*) > 1) x
UNION ALL
SELECT 'germline_dup', COUNT(*) FROM (
    SELECT match_key FROM history_germline WHERE key_version = 4
    GROUP BY match_key HAVING COUNT(*) > 1) y;
