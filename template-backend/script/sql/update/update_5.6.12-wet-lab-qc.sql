-- ============================================================================
-- 湿实验质控表（wet_lab_qc）+ 「湿实验质控」菜单权限点收敛（2026-10-12）
-- 用途：湿实验质控改由接口推数据（POST /qc/wetLabQc/receive），一行 = 样本 × 质控项，
--       质控项与阈值（湿实验阈值）落库时从 qc_standard 快照，实测值由接口给。
-- 已有库直接跑本文件（幂等）；全新安装由 business/wet-lab-qc.sql 走同一份语句。
-- ============================================================================

-- 背景：湿实验质控改由上游接口推数据（POST /qc/wetLabQc/receive），一行 = 样本 × 质控项：
--       质控项/阈值（即「湿实验阈值」）在落库时从 qc_standard 快照过来，实测值由接口给。
-- 依据：docs/context/设计文档/spec.md §1.1（qc_record 字段表跑的是同一条判定链路）、
--       docs/context/流程图/wetlab-qc-flow.md；阈值来源 = qc_standard（qc_category='wet_lab'）。
-- 规范：ai-rules/04-db-schema.md（主键自增 / 公共字段 / 租户列 / 索引）  模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）
-- 幂等：表用 IF NOT EXISTS；菜单按 path/perms 判存在或按旧值 UPDATE，可重复执行。

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 建表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `wet_lab_qc` (
    -- 主键：单列 bigint 自增（全局 idType=AUTO，主键列必须 AUTO_INCREMENT）
    id            bigint       NOT NULL AUTO_INCREMENT           COMMENT '主键',

    -- 业务字段
    subbarcode    varchar(80)  NOT NULL                          COMMENT '样本条码（关联 sample_file.subbarcode）',
    qc_item       varchar(100) NOT NULL                          COMMENT '质控项目名称（中文展示名，落库时取 qc_standard.qc_item）',
    qc_param      varchar(100) NOT NULL                          COMMENT '质控参数（英文原始字段名，落库时取 qc_standard.qc_param；接口按它匹配标准）',
    min_value     varchar(50)                                    COMMENT '合格下限（阈值快照，取 qc_standard.min_value）',
    max_value     varchar(50)                                    COMMENT '合格上限（阈值快照，取 qc_standard.max_value）',
    unit          varchar(30)                                    COMMENT '单位（阈值快照，取 qc_standard.unit）',
    qc_result     varchar(50)                                    COMMENT '实测值（接口推送；非数值原样存，如 A/B/C）',

    -- 公共字段（04-db-schema §2；无 create_dept）
    create_by     bigint                                         COMMENT '创建者',
    create_time   datetime                                       COMMENT '创建时间',
    update_by     bigint                                         COMMENT '更新者',
    update_time   datetime                                       COMMENT '更新时间',
    del_flag      char(1)      NOT NULL DEFAULT '0'              COMMENT '删除标志（0代表存在 1代表删除）',

    -- 租户列：所有业务表必备
    tenant_id     varchar(20)  NOT NULL DEFAULT '000000'         COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 业务唯一键：同一租户下「一个样本的一个质控项」只有一行，接口重复推送按它 upsert（幂等）
    UNIQUE KEY uk_wet_lab_qc_subbarcode_param (tenant_id, subbarcode, qc_param)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='湿实验质控';


-- ============================================================================
-- 2. 「湿实验质控」菜单权限点收敛到本模块（qc:qcRecord:* → qc:wetLabQc:*）
--    原菜单/按钮行由 business/qc-menu.sql 建（本文件按文件名排在其后执行），这里原地改 perms
--    （qc:qcRecord:* 这套旧点位已随 qc_record 下线在 update_5.6.13 里清掉，本段只对还没跑过 5.6.13 的库生效）
--    （menu_id 不变 → 角色授权不用重做）。perms 与 WetLabQcController 的 @SaCheckPermission 逐字一致。
--    幂等：按旧值 UPDATE，第二次执行匹配不到就是空操作。
-- ============================================================================
SET @qc_parent_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'qc' LIMIT 1);
SET @wet_menu_id  = (SELECT menu_id FROM sys_menu WHERE parent_id = @qc_parent_id AND path = 'wet-lab' LIMIT 1);

UPDATE sys_menu SET perms = 'qc:wetLabQc:list', update_by = 1, update_time = NOW()
WHERE menu_id = @wet_menu_id AND perms <> 'qc:wetLabQc:list';

UPDATE sys_menu SET perms = 'qc:wetLabQc:query', update_by = 1, update_time = NOW()
WHERE parent_id = @wet_menu_id AND perms = 'qc:qcRecord:query';

UPDATE sys_menu SET perms = 'qc:wetLabQc:remove', update_by = 1, update_time = NOW()
WHERE parent_id = @wet_menu_id AND perms = 'qc:qcRecord:remove';

-- 接口接收（F）：本模块新增的点位，缺行时补建，并授权给超级管理员角色
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '接口接收', @wet_menu_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'qc:wetLabQc:receive', '#', 1, NOW(), '接口推送'
FROM (SELECT 1) AS dummy
WHERE @wet_menu_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @wet_menu_id AND perms = 'qc:wetLabQc:receive') AS exists_check);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.menu_id FROM sys_menu m
WHERE m.parent_id = @wet_menu_id AND m.perms IN ('qc:wetLabQc:receive', 'qc:wetLabQc:query', 'qc:wetLabQc:remove')
  AND m.menu_id NOT IN (SELECT menu_id FROM (SELECT menu_id FROM sys_role_menu WHERE role_id = 1) AS granted);
