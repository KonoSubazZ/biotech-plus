-- =====================================================================================
-- 产品编码归一后，历史匹配表 project_code / match_key 同步（否则历史复用与临床意义继承会断）
--
-- 背景：5.6.32 把 analysis_data.product 映射成 product_bio（novopm2_blo_1238_shengyu → novopm2_blo_1238），
-- 而 VariantMatcher 的匹配键是
--   sha256("v2|<productCode>|<disease: id|name>|<gender>|<gene>|<variant>|<oriVariant>|<parentIds>")
-- （PreviewSupport.sha256 = UTF-8 小写 hex；nz() = null→''、其余 trim）。project_code 同时出现在
-- selectInheritedGermlineSignificance 的 where 里。不归一：新写入查不到老行 → 历史复用/意义继承失效。
--
-- 处理顺序（关键）：
--   1. 归一后会出现「同一变异既有 _shengyu 行、又有无后缀行」的撞键（somatic 2 组、germline 1 组），
--      唯一键 uk_*_match_key 不允许并存 → 先把每组里**非保留行**的 match_key 迁到 'legacy:<id>'（保留审计、
--      不再参与匹配），保留行取 reuse_count 最大（并列取 id 最大）；
--   2. 再对剩余 _shengyu 行重算 match_key（v1 行不重算：旧口径已不可命中）；
--   3. project_code 去 _shengyu（含 v1 行，展示/审计一致）；
--   4. report_variant_history.product、history_reuse_log.project_code 去 _shengyu（纯展示）；
--   5. 列注释写清 project_code 语义。
-- 公式已用 Python 复算校验：v2 行 69/69（somatic）、26/26（germline）与库内 match_key 完全一致。
-- =====================================================================================

USE `ry-vue`;

-- -------------------------------------------------------------------------------------
-- 1. history_somatic：先把撞键的非保留行挪走
-- -------------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_dup_somatic;
CREATE TEMPORARY TABLE tmp_dup_somatic AS
SELECT k,
       CAST(SUBSTRING_INDEX(GROUP_CONCAT(id ORDER BY reuse_count DESC, id DESC), ',', 1) AS UNSIGNED) AS keeper
FROM (
    SELECT id, reuse_count, SHA2(CONCAT(
               'v2', '|', TRIM(REPLACE(project_code, '_shengyu', '')), '|',
               IF(disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(disease, ''))), CONCAT('id:', disease_id)), '|',
               TRIM(IFNULL(gender, '')), '|', TRIM(gene), '|', TRIM(variant), '|',
               TRIM(IFNULL(ori_variant, '')), '|', TRIM(IFNULL(parent_mutation_id, ''))), 256) AS k
    FROM history_somatic
    WHERE key_version = 2
) t
GROUP BY k
HAVING COUNT(*) > 1;

UPDATE history_somatic h
    JOIN tmp_dup_somatic d
      ON d.k = SHA2(CONCAT(
             'v2', '|', TRIM(REPLACE(h.project_code, '_shengyu', '')), '|',
             IF(h.disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(h.disease, ''))), CONCAT('id:', h.disease_id)), '|',
             TRIM(IFNULL(h.gender, '')), '|', TRIM(h.gene), '|', TRIM(h.variant), '|',
             TRIM(IFNULL(h.ori_variant, '')), '|', TRIM(IFNULL(h.parent_mutation_id, ''))), 256)
     AND h.id <> d.keeper
SET h.match_key = CONCAT('legacy:', h.id)
WHERE h.key_version = 2;

UPDATE history_somatic
SET match_key = SHA2(CONCAT(
        'v2', '|', TRIM(REPLACE(project_code, '_shengyu', '')), '|',
        IF(disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(disease, ''))), CONCAT('id:', disease_id)), '|',
        TRIM(IFNULL(gender, '')), '|', TRIM(gene), '|', TRIM(variant), '|',
        TRIM(IFNULL(ori_variant, '')), '|', TRIM(IFNULL(parent_mutation_id, ''))), 256)
WHERE key_version = 2
  AND project_code LIKE '%\_shengyu';

UPDATE history_somatic
SET project_code = TRIM(REPLACE(project_code, '_shengyu', ''))
WHERE project_code LIKE '%\_shengyu';

-- -------------------------------------------------------------------------------------
-- 2. history_germline：同上
-- -------------------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS tmp_dup_germline;
CREATE TEMPORARY TABLE tmp_dup_germline AS
SELECT k,
       CAST(SUBSTRING_INDEX(GROUP_CONCAT(id ORDER BY reuse_count DESC, id DESC), ',', 1) AS UNSIGNED) AS keeper
FROM (
    SELECT id, reuse_count, SHA2(CONCAT(
               'v2', '|', TRIM(REPLACE(project_code, '_shengyu', '')), '|',
               IF(disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(disease, ''))), CONCAT('id:', disease_id)), '|',
               TRIM(IFNULL(gender, '')), '|', TRIM(gene), '|', TRIM(variant), '|',
               TRIM(IFNULL(ori_variant, '')), '|', TRIM(IFNULL(parent_mutation_id, ''))), 256) AS k
    FROM history_germline
    WHERE key_version = 2
) t
GROUP BY k
HAVING COUNT(*) > 1;

UPDATE history_germline h
    JOIN tmp_dup_germline d
      ON d.k = SHA2(CONCAT(
             'v2', '|', TRIM(REPLACE(h.project_code, '_shengyu', '')), '|',
             IF(h.disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(h.disease, ''))), CONCAT('id:', h.disease_id)), '|',
             TRIM(IFNULL(h.gender, '')), '|', TRIM(h.gene), '|', TRIM(h.variant), '|',
             TRIM(IFNULL(h.ori_variant, '')), '|', TRIM(IFNULL(h.parent_mutation_id, ''))), 256)
     AND h.id <> d.keeper
SET h.match_key = CONCAT('legacy:', h.id)
WHERE h.key_version = 2;

UPDATE history_germline
SET match_key = SHA2(CONCAT(
        'v2', '|', TRIM(REPLACE(project_code, '_shengyu', '')), '|',
        IF(disease_id IS NULL, CONCAT('name:', TRIM(IFNULL(disease, ''))), CONCAT('id:', disease_id)), '|',
        TRIM(IFNULL(gender, '')), '|', TRIM(gene), '|', TRIM(variant), '|',
        TRIM(IFNULL(ori_variant, '')), '|', TRIM(IFNULL(parent_mutation_id, ''))), 256)
WHERE key_version = 2
  AND project_code LIKE '%\_shengyu';

UPDATE history_germline
SET project_code = TRIM(REPLACE(project_code, '_shengyu', ''))
WHERE project_code LIKE '%\_shengyu';

-- -------------------------------------------------------------------------------------
-- 3. 展示列与审计日志
-- -------------------------------------------------------------------------------------
UPDATE report_variant_history
SET product = TRIM(REPLACE(product, '_shengyu', ''))
WHERE product LIKE '%\_shengyu';

UPDATE history_reuse_log
SET project_code = TRIM(REPLACE(project_code, '_shengyu', ''))
WHERE project_code LIKE '%\_shengyu';

-- -------------------------------------------------------------------------------------
-- 4. 列注释
-- -------------------------------------------------------------------------------------
ALTER TABLE history_somatic
    MODIFY COLUMN project_code VARCHAR(120) DEFAULT NULL
        COMMENT '产品项目编码（= analysis_data.product_bio，参与 v2 匹配键；归属展示请看 project_no）';

ALTER TABLE history_germline
    MODIFY COLUMN project_code VARCHAR(120) DEFAULT NULL
        COMMENT '产品项目编码（= analysis_data.product_bio，参与 v2 匹配键；归属展示请看 project_no）';

ALTER TABLE history_reuse_log
    MODIFY COLUMN project_code VARCHAR(120) DEFAULT NULL
        COMMENT '产品项目编码（= analysis_data.product_bio；历史日志按当时值保留）';

-- -------------------------------------------------------------------------------------
-- 5. 核对
-- -------------------------------------------------------------------------------------
SELECT 'somatic_old' AS t, COUNT(*) AS n FROM history_somatic WHERE project_code LIKE '%\_shengyu'
UNION ALL SELECT 'germline_old', COUNT(*) FROM history_germline WHERE project_code LIKE '%\_shengyu'
UNION ALL SELECT 'rvh_old', COUNT(*) FROM report_variant_history WHERE product LIKE '%\_shengyu'
UNION ALL SELECT 'somatic_legacy_rows', COUNT(*) FROM history_somatic WHERE match_key LIKE 'legacy:%'
UNION ALL SELECT 'germline_legacy_rows', COUNT(*) FROM history_germline WHERE match_key LIKE 'legacy:%';

SELECT 'somatic_dup_keys' AS t, COUNT(*) AS n FROM (
    SELECT match_key FROM history_somatic WHERE key_version = 2 AND match_key NOT LIKE 'legacy:%'
    GROUP BY match_key HAVING COUNT(*) > 1) x
UNION ALL
SELECT 'germline_dup_keys', COUNT(*) FROM (
    SELECT match_key FROM history_germline WHERE key_version = 2 AND match_key NOT LIKE 'legacy:%'
    GROUP BY match_key HAVING COUNT(*) > 1) y;
