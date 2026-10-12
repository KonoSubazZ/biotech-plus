-- ============================================================================
-- 湿实验质控表形态调整：长表（一行 = 样本 × 质控项）→ **宽表（一行 = 一个样本，每个质控项一列）**
--   （与生信质控 bioinfo_qc 对齐；用户口径：湿实验的展示要一个样本一行数据）
-- 列名 = qc_standard.qc_param（质控参数/英文原始字段名），中文名在 qc_standard.qc_item 里
-- 因为表刚建、里面只有自测灌的演示数据，这里直接 DROP 重建（不写字段搬运）：
--   ⚠️ 执行前旧表的行会丢；若不是演示数据，先
--      docker exec biotech-plus-mysql mysqldump -uroot -p*** ry-vue wet_lab_qc > wet_lab_qc_bak.sql
-- 幂等：DROP TABLE IF EXISTS + CREATE TABLE IF NOT EXISTS，可重复执行（第二次不再动数据）。
-- 配套：business/wet-lab-qc.sql 是同一份定义（新装库用）。
-- ============================================================================

SET NAMES utf8mb4;

DROP TABLE IF EXISTS `wet_lab_qc`;

CREATE TABLE IF NOT EXISTS `wet_lab_qc` (
    `id`                     bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `subbarcode`             varchar(80)  NOT NULL                COMMENT '样本条码（关联 sample_file.subbarcode）',

    -- 业务字段：一个质控项一列，列名 = qc_standard.qc_param（英文原始字段名）
    `patho_tumor_purity`     varchar(50)                          COMMENT '肿瘤纯度',
    `dna_total_ng`           varchar(50)                          COMMENT 'DNA总量（ng）',
    `dna_degradation`        varchar(50)                          COMMENT '检测结果判断（等级 A/B/C/D）',
    `pre_lib_total_ng`       varchar(50)                          COMMENT '预文库总量（ng）',
    `final_lib_conc`         varchar(50)                          COMMENT '终文库浓度（ng/uL）',
    `dna_total_ng_normal`    varchar(50)                          COMMENT 'DNA总量（ng）对照',
    `pre_lib_total_ng_normal` varchar(50)                         COMMENT '预文库总量（ng）对照',
    `final_lib_conc_normal`  varchar(50)                          COMMENT '终文库浓度（ng/uL）对照',

    -- 公共字段
    `create_by`              bigint                               COMMENT '创建者',
    `create_time`            datetime                             COMMENT '创建时间',
    `update_by`              bigint                               COMMENT '更新者',
    `update_time`            datetime                             COMMENT '更新时间',
    `del_flag`               char(1)      NOT NULL DEFAULT '0'    COMMENT '删除标志（0代表存在 1代表删除）',
    `tenant_id`              varchar(20)  NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_wet_lab_qc_subbarcode` (`tenant_id`, `subbarcode`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='湿实验质控（一行一个样本，列名=qc_param）';

-- 核对
SELECT COUNT(*) AS wet_lab_qc列数 FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'wet_lab_qc';
