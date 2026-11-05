-- =====================================================================================
-- ⚠ 仅测试环境：清掉匹配键 v3 之前的历史遗留行
--
-- 背景：v3 匹配键（项目编号 + 产品项目 + 癌种 + 性别 + 位点）上线后，v1 / v2 行已不可命中；
-- 5.6.37 归一撞键时保留的 legacy:<id> 行也只是一次性审计残留。
-- 用户 2026-10-22 明确：测试系统不做兼容，破坏性更新直接重建报告与测试数据。
--
-- 生产不要执行本脚本：生产先按 5.6.37 / 5.6.38 的口径评估历史复用影响再决定是否清理。
-- =====================================================================================

USE `ry-vue`;

DELETE FROM `history_somatic`  WHERE `key_version` <> 3 OR `match_key` LIKE 'legacy:%';
DELETE FROM `history_germline` WHERE `key_version` <> 3 OR `match_key` LIKE 'legacy:%';
DELETE FROM `history_reuse_log`;

SELECT 'history_somatic' AS t, COUNT(*) AS n FROM `history_somatic`
UNION ALL SELECT 'history_germline', COUNT(*) FROM `history_germline`
UNION ALL SELECT 'history_reuse_log', COUNT(*) FROM `history_reuse_log`
UNION ALL SELECT 'report_variant_history', COUNT(*) FROM `report_variant_history`;
