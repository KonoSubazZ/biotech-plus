-- ============================================================================
-- 湿实验质控 wet_lab_qc —— **宽表：一行一个样本，每个质控项一列**（形态对齐生信质控 bioinfo_qc）
-- 列名 = 质控标准里的 qc_param（英文原始字段名），中文名在 qc_standard.qc_item 里维护
--   （qc_param 既是标准与实测值的对齐键，也是本表的列名 —— 判定时按它取阈值比对）
-- 数据来源：上游湿实验流程推送（POST /qc/wetLabQc/receive），落库按 cms_sample 解析归属药企
-- 当前列（与 qc_standard 里启用的 wet_lab 标准一一对应；新增质控项要同时加列 + 加标准）：
--   patho_tumor_purity        肿瘤纯度
--   dna_total_ng              DNA总量（ng）
--   dna_degradation           检测结果判断（等级 A/B/C/D）
--   pre_lib_total_ng          预文库总量（ng）
--   final_lib_conc            终文库浓度（ng/uL）
--   dna_total_ng_normal       DNA总量（ng）对照
--   pre_lib_total_ng_normal   预文库总量（ng）对照
--   final_lib_conc_normal     终文库浓度（ng/uL）对照
-- 值列统一 varchar(50)：阈值本身（qc_standard.min_value/max_value）就是 varchar，等级值（B）、
--   带比较符号的值都能原样存；以后要参与数值计算再按列改类型。
-- 幂等：CREATE TABLE IF NOT EXISTS + 菜单段按 path/perms 定位；脚本可重复执行。
-- 体检：python3 tools/check_db_schema.py

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `wet_lab_qc` (
    `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subbarcode`             varchar(80)  NOT NULL                COMMENT '样本条码（关联 cms_sample.subbarcode）',

    -- 业务字段：一个质控项一列，列名 = qc_standard.qc_param（英文原始字段名）
    `patho_tumor_purity`     varchar(50)                          COMMENT '肿瘤纯度',
    `dna_total_ng`           varchar(50)                          COMMENT 'DNA总量（ng）',
    `dna_degradation`        varchar(50)                          COMMENT '检测结果判断（等级 A/B/C/D）',
    `pre_lib_total_ng`       varchar(50)                          COMMENT '预文库总量（ng）',
    `final_lib_conc`         varchar(50)                          COMMENT '终文库浓度（ng/uL）',
    `dna_total_ng_normal`    varchar(50)                          COMMENT 'DNA总量（ng）对照',
    `pre_lib_total_ng_normal` varchar(50)                         COMMENT '预文库总量（ng）对照',
    `final_lib_conc_normal`  varchar(50)                          COMMENT '终文库浓度（ng/uL）对照',

    -- 公共字段（两张质控表共用，口径见 business/bioinfo-qc.sql 的设计说明）：
    --   status 按 qc_standard 阈值由服务端判定；notify_status 记录通知发送状态（暂不展示）
    `status`                 varchar(20)  NOT NULL DEFAULT 'pending' COMMENT '质控状态（passed 合格 / failed 不合格 / pending 待判定）',
    `notify_status`          varchar(20)                          COMMENT '通知发送状态（已生成（文档）/ 已发送（邮件））',

    -- 公共字段
    `create_by`              bigint                               COMMENT '创建者',
    `create_time`            datetime                             COMMENT '创建时间',
    `update_by`              bigint                               COMMENT '更新者',
    `update_time`            datetime                             COMMENT '更新时间',
    `del_flag`               char(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`              varchar(20)  NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    -- 一行一个样本：接口按 (租户, 样本条码) upsert，重复推送覆盖而不是插新行
    UNIQUE KEY `uk_wet_lab_qc_subbarcode` (`tenant_id`, `subbarcode`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='湿实验质控（一行一个样本，列名=qc_param）';


-- ============================================================================
-- 菜单权限点收敛：「湿实验质控」菜单与按钮沿用既有 menu_id，原地把 perms 改成 qc:wetLabQc:*
--   （menu_id 不变 → 角色授权不用重做；qc:qcRecord:* 那套旧点位已随 qc_record 下线在
--     update_5.6.13 里清掉，本段只对还没跑过 5.6.13 的库生效）
--   新装库的菜单/权限点由 business/qc-menu.sql 建（本文件按文件名排在其后），这里匹配不到就是空操作
-- ============================================================================
UPDATE sys_menu SET perms = 'qc:wetLabQc:list', menu_name = 'route.qc_wet-lab',
    component = 'qc/wet-lab/index', update_by = 1, update_time = NOW()
WHERE perms = 'qc:qcRecord:list' AND component = 'qc/wet-lab/index';

UPDATE sys_menu SET perms = 'qc:wetLabQc:query', update_by = 1, update_time = NOW()
WHERE perms = 'qc:qcRecord:query' AND parent_id = (
    SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE component = 'qc/wet-lab/index' LIMIT 1) AS t);

UPDATE sys_menu SET perms = 'qc:wetLabQc:remove', update_by = 1, update_time = NOW()
WHERE perms = 'qc:qcRecord:remove' AND parent_id = (
    SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE component = 'qc/wet-lab/index' LIMIT 1) AS t);

-- 新增的接口权限点（原模块没有这个点位），插在「湿实验质控」菜单下
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '接口接收', m.menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'qc:wetLabQc:receive', '#', 1, NOW(), '接口推送'
FROM sys_menu m
WHERE m.component = 'qc/wet-lab/index'
  AND NOT EXISTS (SELECT 1 FROM sys_menu x WHERE x.parent_id = m.menu_id AND x.perms = 'qc:wetLabQc:receive');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m WHERE m.perms = 'qc:wetLabQc:receive';
