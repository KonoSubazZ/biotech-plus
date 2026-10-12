-- 生信质控（宽表 bioinfo_qc）+ 与湿实验质控共用的公共字段约定 + 菜单/权限点位
-- 依据：docs/context/设计文档/bioinfo-qc.md；
--       字段来源 /home/liushangzhi/project/sample_qc_info.sql（11 行 × 41 列的样例数据）
-- 规范：ai-rules/04-db-schema.md（主键自增 / 公共字段 / 租户列 / 索引 / 排序规则）
-- 模板：ai-templates/db/table-template.sql
-- 体检：python3 tools/check_db_schema.py   （error 必须为 0）
--
-- 设计说明（两张质控表共用的「公共字段」规则，湿实验质控 wet_lab_qc 后续照抄本段）：
--   id          主键（bigint 自增，页面不展示）
--   subbarcode  样本编号（= sample_file.subbarcode，源 sample_qc_info.sample_id），业务唯一键
--   source_id   上游记录 id（源表 id，幂等/追溯用，页面不展示）
--   status      质控状态：passed 合格 / failed 不合格 / pending 待判定（按 qc_standard 阈值判定，阈值可改）
--   notify_status  通知发送状态：已生成（文档）/ 已发送（邮件）；页面暂不展示（湿实验表同口径）
--   create_by / create_time / update_by / update_time / del_flag / tenant_id  审计与租户列
-- 数据来源：全部由接口灌入（POST /qc/bioinfo/receive），页面只读，不提供新增/修改/删除。

SET NAMES utf8mb4;

-- ============================================================================
-- 1. 建表
-- ============================================================================
CREATE TABLE IF NOT EXISTS `bioinfo_qc` (
    -- ── 主键：单列 bigint 自增（全局 idType=AUTO，主键列必须 AUTO_INCREMENT）
    id                                    bigint        NOT NULL AUTO_INCREMENT     COMMENT '主键',

    -- ── 公共字段（两张质控表共用）
    subbarcode                            varchar(100)  NOT NULL                    COMMENT '样本编号（= sample_file.subbarcode，源 sample_qc_info.sample_id）',
    source_id                             varchar(64)                               COMMENT '上游记录 id（源 sample_qc_info.id，幂等/追溯用）',
    status                                varchar(20)   NOT NULL DEFAULT 'pending'  COMMENT '质控状态（passed 合格 / failed 不合格 / pending 待判定）',
    notify_status                         varchar(20)                              COMMENT '通知发送状态（已生成（文档）/ 已发送（邮件）），暂不展示',

    -- ── 生信专属字段（列名与源表 sample_qc_info 一致；暂无中文名，按字段名展示）
    bam                                   varchar(255)                              COMMENT 'bam',
    gender                                varchar(10)                               COMMENT 'gender',
    lib_id                                varchar(255)                              COMMENT 'lib_id',
    paired                                varchar(20)                               COMMENT 'paired',
    raw_gc                                decimal(10,2)                             COMMENT 'raw_gc',
    hot_pos                               varchar(100)                              COMMENT 'hot_pos',
    raw_q20                               decimal(10,2)                             COMMENT 'raw_q20',
    raw_q30                               decimal(10,2)                             COMMENT 'raw_q30',
    pipeline                              varchar(50)                               COMMENT 'pipeline',
    raw_base                              decimal(10,2)                             COMMENT 'raw_base',
    ssh_path                              varchar(500)                              COMMENT 'ssh_path',
    duplicate                             decimal(10,2)                             COMMENT 'duplicate',
    peak_size                             decimal(10,2)                             COMMENT 'peak_size',
    qc_result                             varchar(100)                              COMMENT 'qc_result',
    sqi16                                 varchar(50)                               COMMENT 'sqi16',
    bio_result                            varchar(50)                               COMMENT 'bio_result',
    peak_range                            varchar(50)                               COMMENT 'peak_range',
    concordance                           varchar(50)                               COMMENT 'concordance',
    chrall                                json                                      COMMENT 'chrall',
    library_size                          decimal(12,2)                             COMMENT 'library_size',
    bam_realign                           varchar(255)                              COMMENT 'bam_realign',
    target_size                           bigint                                    COMMENT 'target_size',
    total_reads                           bigint                                    COMMENT 'total_reads',
    mapping_rate                          decimal(10,2)                             COMMENT 'mapping_rate',
    average_depth                         decimal(12,2)                             COMMENT 'average_depth',
    coverage_target                       decimal(10,2)                             COMMENT 'coverage_target',
    mapped_reads                          bigint                                    COMMENT 'mapped_reads',
    duplicate_target                      decimal(10,2)                             COMMENT 'duplicate_target',
    contamination_level                   decimal(10,4)                             COMMENT 'contamination_level（源值带 %，入库去掉）',
    coverage_target_50x                   decimal(10,2)                             COMMENT 'coverage_target_50x',
    probe_capture_ratio                   decimal(10,2)                             COMMENT 'probe_capture_ratio',
    coverage_target_500x                  decimal(10,2)                             COMMENT 'coverage_target_500x',
    coverage_target_1000x                 decimal(10,2)                             COMMENT 'coverage_target_1000x',
    coverage_uniformity_02xad_on_target   decimal(10,2)                             COMMENT 'coverage_uniformity_02xad_on_target',
    before_duplicate                      bigint                                    COMMENT 'before_duplicate',

    -- ── 公共字段（审计 / 软删 / 租户）
    create_by                             bigint                                    COMMENT '创建者',
    create_time                           datetime                                  COMMENT '创建时间',
    update_by                             bigint                                    COMMENT '更新者',
    update_time                           datetime                                  COMMENT '更新时间',
    del_flag                              char(1)       NOT NULL DEFAULT '0'        COMMENT '删除标志（0代表存在 1代表删除）',
    tenant_id                             varchar(20)   NOT NULL DEFAULT '000000'   COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    -- 业务唯一键：一个样本一条质控记录（重复推送按它 upsert）
    UNIQUE KEY uk_bioinfo_qc_subbarcode (tenant_id, subbarcode),
    -- 搜索栏按 lib_id 查
    KEY idx_bioinfo_qc_lib_id (tenant_id, lib_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='生信质控';


-- ============================================================================
-- 2. 判定标准项名对齐字段名：qc_standard.qc_item 用 row_base，源数据列叫 raw_base
--    阈值判定是「标准项名 == 表列名」逐项比对，所以把标准项名改成 raw_base（根因修，代码里不留别名）
--    幂等：只在目标产品还没有 raw_base 标准项时改（否则会撞唯一键 uk_qc_standard_product_item_category）
-- ============================================================================
UPDATE `qc_standard` s
SET s.qc_item = 'raw_base'
WHERE s.qc_item = 'row_base'
  AND s.qc_category = 'bioinfo'
  AND NOT EXISTS (
        SELECT 1 FROM (
            SELECT tenant_id, product_id FROM `qc_standard`
            WHERE qc_item = 'raw_base' AND qc_category = 'bioinfo'
        ) AS existing
        WHERE existing.tenant_id = s.tenant_id AND existing.product_id = s.product_id
      );


-- ============================================================================
-- 3. 菜单与权限点位
--    生信质控菜单（menu_id 1641，路由 /qc/bioinfo）沿用；列表点位改为 qc:bioinfo:list，
--    并补查询 / 接口接收两个按钮权限（perms 与 BioinfoQcController 的 @SaCheckPermission 逐字一致）
--    幂等：先 UPDATE 旧值，再 NOT EXISTS 插新行，最后把菜单已有的角色授权复制到新权限
-- ============================================================================
UPDATE `sys_menu` SET perms = 'qc:bioinfo:list' WHERE menu_id = 1641 AND perms = 'qc:qcRecord:list';

SET @menu_id = 1641;

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '查询', @menu_id, 1, '', NULL, 1, 0, 'F', '0', '0', 'qc:bioinfo:query', '#', 1, NOW(), '查询'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu`
    WHERE parent_id = @menu_id AND perms = 'qc:bioinfo:query') AS c1);

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '接口接收', @menu_id, 2, '', NULL, 1, 0, 'F', '0', '0', 'qc:bioinfo:receive', '#', 1, NOW(), '接口接收'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu`
    WHERE parent_id = @menu_id AND perms = 'qc:bioinfo:receive') AS c2);

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '修改', @menu_id, 3, '', NULL, 1, 0, 'F', '0', '0', 'qc:bioinfo:edit', '#', 1, NOW(), '修改'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu`
    WHERE parent_id = @menu_id AND perms = 'qc:bioinfo:edit') AS c3);

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '删除', @menu_id, 4, '', NULL, 1, 0, 'F', '0', '0', 'qc:bioinfo:remove', '#', 1, NOW(), '删除'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu`
    WHERE parent_id = @menu_id AND perms = 'qc:bioinfo:remove') AS c4);

-- 把「生信质控菜单已经授权给的角色」同步授权到新增的按钮权限（当前 = 超级管理员 1、报告解读师 4）
INSERT IGNORE INTO `sys_role_menu` (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON m.parent_id = 1641
    AND m.perms IN ('qc:bioinfo:query', 'qc:bioinfo:receive', 'qc:bioinfo:edit', 'qc:bioinfo:remove')
WHERE rm.menu_id = 1641;


-- ============================================================================
-- 4. 自检
-- ============================================================================
SELECT table_name, table_comment FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name = 'bioinfo_qc';

SELECT menu_id, parent_id, menu_name, perms FROM `sys_menu`
WHERE menu_id = 1641 OR parent_id = 1641 ORDER BY menu_id;
