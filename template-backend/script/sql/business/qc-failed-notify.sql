-- 样本质控不合格通知（处理记录）：生信/湿实验质控判定不合格后，走「处理」弹窗生成通知文档时留痕
-- 为什么单独一张表：通知文档的字段来自 sample_file + 质控记录 + 人工填写三处，
--   且生成出来的 JSON / DOCX 路径要能追溯（合规要求「生成记录流存」）。
-- 关系（用户口径）：sample_file 一定有；湿实验不合格 → 没有生信质控；湿实验合格 → 生信可能合格/不合格。
-- 每次「生成」追加一条记录（流水留痕），同一张通知单可重新生成多份；按 subbarcode 取最新一条做回显。
-- 体检：python3 tools/check_db_schema.py

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `qc_failed_notify` (
    id                bigint        NOT NULL AUTO_INCREMENT  COMMENT '主键',

    subbarcode        varchar(100)  NOT NULL                 COMMENT '样本编号（= sample_file.subbarcode）',
    sample_file_id    bigint                  DEFAULT NULL   COMMENT '样本信息 id（一定有）',
    wet_lab_qc_id     bigint                  DEFAULT NULL   COMMENT '湿实验质控 id（湿实验不合格时才有）',
    bioinfo_qc_id     bigint                  DEFAULT NULL   COMMENT '生信质控 id（湿实验合格后才可能有）',
    qc_category       varchar(20)             DEFAULT NULL   COMMENT '质控类型（wet_lab 湿实验 / bioinfo 生信）—— 在哪个质控页点处理产生的',

    notify_no         varchar(64)             DEFAULT NULL   COMMENT '通知单号（样本编号 + 生成时间）',
    notify_status     varchar(20)  NOT NULL DEFAULT '已生成（文档）' COMMENT '通知状态：已生成（文档）/ 已发送（邮件）',
    notify_date       varchar(32)             DEFAULT NULL   COMMENT '日期（生成当天 yyyy-MM-dd）',
    analysis_time     varchar(32)             DEFAULT NULL   COMMENT '分析时间（质控记录入库时间，自动带出）',
    product           varchar(256)            DEFAULT NULL   COMMENT '检测产品',
    customer_name     varchar(100)            DEFAULT NULL   COMMENT '客户',
    sample_type       varchar(100)            DEFAULT NULL   COMMENT '样本类型',

    qc_stage          varchar(200)            DEFAULT NULL   COMMENT '不合格环节（多选，中文逗号分隔）',
    terminate_measure varchar(200)            DEFAULT NULL   COMMENT '是否终止检测（多选，中文逗号分隔）',
    remedy            varchar(300)            DEFAULT NULL   COMMENT '建议处理办法（多选，中文逗号分隔）',
    remedy_other      varchar(500)            DEFAULT NULL   COMMENT '建议处理办法-其他（原文）',
    reason            varchar(500)            DEFAULT NULL   COMMENT '不合格原因',

    reporter          varchar(100)            DEFAULT NULL   COMMENT '反馈人',
    report_time       varchar(32)             DEFAULT NULL   COMMENT '反馈时间',
    reviewer          varchar(100)            DEFAULT NULL   COMMENT '复核人',
    review_time       varchar(32)             DEFAULT NULL   COMMENT '复核时间',

    json_path         varchar(500)            DEFAULT NULL   COMMENT '渲染用的 JSON 路径',
    raw_docx_path     varchar(500)            DEFAULT NULL   COMMENT '生成的 DOCX 路径',
    pdf_path          varchar(500)            DEFAULT NULL   COMMENT 'PDF 路径（OnlyOffice 转换产物；转换失败时为空）',
    sign_contract_id  varchar(64)             DEFAULT NULL   COMMENT '上上签合同号（送签后回填）',
    signed_pdf_path   varchar(500)            DEFAULT NULL   COMMENT '签名后PDF路径（上上签回传）',

    create_by         bigint                  DEFAULT NULL   COMMENT '创建者',
    create_time       datetime                DEFAULT NULL   COMMENT '创建时间',
    update_by         bigint                  DEFAULT NULL   COMMENT '更新者',
    update_time       datetime                DEFAULT NULL   COMMENT '更新时间',
    del_flag          char(1)      NOT NULL DEFAULT '0'      COMMENT '删除标志（0存在 1删除）',
    tenant_id         varchar(20)  NOT NULL DEFAULT '000000'   COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_qc_failed_notify_subbarcode (subbarcode),
    KEY idx_tenant_id (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '样本质控不合格通知（生成记录）';

-- 口径对齐（模板要求 NOT NULL DEFAULT）：老环境先建过的话补一条
-- 口径对齐（模板要求 NOT NULL DEFAULT）：老环境先建过的话补一条（MODIFY 可重复执行）
ALTER TABLE `qc_failed_notify`
    MODIFY COLUMN `del_flag` char(1) NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    MODIFY COLUMN `tenant_id` varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号';
-- 注意：`notify_status` 这类「后加的列」不能写成 ADD COLUMN IF NOT EXISTS（MySQL 8 不支持，只有 MariaDB 有），
-- 已经建过表的环境走 script/sql/update/update_5.6.10-qc-failed-notify-status.sql。

-- ----------------------------------------------------------------------------
-- 菜单/按钮权限（挂在「生信质控」菜单 1641 下；湿实验页将来复用同一批点位）
-- ----------------------------------------------------------------------------
SET @menu_id = (SELECT menu_id FROM `sys_menu` WHERE parent_id = 1639 AND component = 'qc/bioinfo/index' LIMIT 1);

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知查询', @menu_id, 5, '', NULL, 1, 0, 'F', '0', '0', 'qc:notify:query', '#', 1, NOW(), '处理弹窗预填/查看'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu`
    WHERE parent_id = @menu_id AND perms = 'qc:notify:query') AS c1);

INSERT INTO `sys_menu` (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT '通知生成', @menu_id, 6, '', NULL, 1, 0, 'F', '0', '0', 'qc:notify:generate', '#', 1, NOW(), '生成不合格通知 DOCX'
FROM (SELECT 1) AS dummy
WHERE @menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM `sys_menu`
    WHERE parent_id = @menu_id AND perms = 'qc:notify:generate') AS c2);

-- 把「已经授权给生信质控菜单」的角色同步授权到这两个按钮
INSERT IGNORE INTO `sys_role_menu` (role_id, menu_id)
SELECT rm.role_id, m.menu_id
FROM `sys_role_menu` rm
JOIN `sys_menu` m ON m.parent_id = 1641 AND m.perms IN ('qc:notify:query', 'qc:notify:generate')
WHERE rm.menu_id = 1641;

SELECT m.menu_id, m.parent_id, m.menu_name, m.perms
FROM `sys_menu` m WHERE m.parent_id = 1641 ORDER BY m.order_num;
