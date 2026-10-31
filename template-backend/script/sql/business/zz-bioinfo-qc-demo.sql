-- 生信质控演示数据：11 条样本（cms_sample）
-- 用途：让「上游推生信质控数据」和「质控不合格通知」两条链路都能跑通 ——
--       POST /qc/bioinfo/receive 只收 subbarcode，归属药企由服务端静默查 cms_sample.customer_name（再匹配 sys_tenant）；
--       通知单表头（研究方案编号/诺禾项目编号/试验参与者编号/研究中心名称/样本接收日期/样本类型/样本数量/检测产品）
--       按 subbarcode 从这里回填（这几列是示例口径，真实系统里由 web_crm 源表同步）。
-- 数据来源：/home/liushangzhi/project/sample_qc_info.sql 的 11 行样例，把源 sample_id 当作样本编号
--           （用户 2026-10-08 口径：sample_id 改叫 subbarcode）。
-- customer_name：前 3 条写药企编号、其余写药企名称 —— sys_tenant 两条匹配路径都能验证。
-- 幂等：INSERT IGNORE + 显式主键（1001-1011）建行，再用 UPDATE 把示例列收敛回权威值（老库重复跑也能补齐）。
-- 体检：python3 tools/check_db_schema.py

SET NAMES utf8mb4;

INSERT IGNORE INTO `cms_sample` (`id`, `subbarcode`, `customer_name`, `sample_type`, `tenant_id`,
    `contracts_no`, `order_code`, `pcode`, `testing_institution`, `collect_date`, `specimen_num`, `erp_test_name`)
VALUES
    (1001, '2085f9ba-cec3-4512-91f3-4cc293a062f7', '0000001', '石蜡切片', '0000001',
     'NGS-2026-001', 'P20260101', 'SUBJ-1001', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1002, '132832f1-8011-415d-9076-04aa482ddf81', '0000001', '石蜡切片', '0000001',
     'NGS-2026-002', 'P20260102', 'SUBJ-1002', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1003, 'da21b99c-d786-41d8-b074-89135893956f', '0000001', '石蜡切片', '0000001',
     'NGS-2026-003', 'P20260103', 'SUBJ-1003', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1004, '4ca4809f-baf4-44bc-9da9-f71e08119323', '圣域药企-测试', '石蜡切片', '0000001',
     'NGS-2026-004', 'P20260104', 'SUBJ-1004', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1005, '1bd60fd5-89fc-4064-ae5b-215acd92e712', '圣域药企-测试', '石蜡切片', '0000001',
     'NGS-2026-005', 'P20260105', 'SUBJ-1005', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1006, 'fe149e90-0579-4c18-bd47-09049d09f1bf', '圣域药企-测试', '石蜡切片', '0000001',
     'NGS-2026-006', 'P20260106', 'SUBJ-1006', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1007, '875297ec-c847-4af2-941a-f3e6d0d0e9cc', '圣域药企-测试', '石蜡切片', '0000001',
     'NGS-2026-007', 'P20260107', 'SUBJ-1007', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1008, '485b0e83-45f7-4492-8e1c-9ed9e6638c1c', '圣域药企-测试', '石蜡切片', '0000001',
     'NGS-2026-008', 'P20260108', 'SUBJ-1008', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1009, '55f2655a-c353-438b-a568-758b6d25e79a', '圣域药企-测试', '石蜡切片', '0000001',
     'NGS-2026-009', 'P20260109', 'SUBJ-1009', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1010, 'ca4c02e8-0a2a-46b4-a2b3-2fd6b08ccc3e', '圣域药企-测试', '石蜡切片', '0000001',
     'NGS-2026-010', 'P20260100', 'SUBJ-1010', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238'),
    (1011, '2e68519e-203f-4022-9bfb-7bfb03ce3456', '圣域药企-测试', '石蜡切片', '0000001',
     'NGS-2026-011', 'P20260101', 'SUBJ-1011', '示例研究中心', '2026-06-10', '1', 'novopm2_tis_1238');

-- 已经存在的老行也要补上示例列（显式主键的 INSERT IGNORE 不会更新已有行）
UPDATE `cms_sample` SET
    `contracts_no`        = CONCAT('NGS-2026-', LPAD(`id` % 1000, 3, '0')),
    `order_code`          = CONCAT('P2026010', `id` % 10),
    `pcode`               = CONCAT('SUBJ-', `id`),
    `testing_institution` = '示例研究中心',
    `collect_date`        = '2026-06-10',
    `specimen_num`        = '1',
    `erp_test_name`       = 'novopm2_tis_1238'
WHERE `id` BETWEEN 1001 AND 1011;

SELECT id, subbarcode, customer_name, contracts_no, order_code, pcode, testing_institution,
       collect_date, specimen_num, erp_test_name, tenant_id
FROM `cms_sample` WHERE id BETWEEN 1001 AND 1011;
