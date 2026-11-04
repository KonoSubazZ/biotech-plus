-- 样本通知台账（四类通知一张表：延期 / 样本不合格 / 湿实验不合格 / 干实验不合格）
-- 来历：由 qc_failed_notify 改名扩列而来（迁移脚本 update_5.6.55-notify-aggregate.sql，保历史数据与自增 id）。
-- 一条记录 = 一张通知单，从生成到发送的全过程都写在同一条上：
--   generated 已生成 → (送签/签署) → pending 待处理（提交后）→ sent 发送成功 / failed 发送失败
-- 文件路径四段全量落库（json / docx / pdf / signed_pdf），file_path 是当前交付件，供发邮件与备份使用。
-- 体检：python3 tools/check_db_schema.py

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `sample_notify` (
    id                    bigint       NOT NULL AUTO_INCREMENT  COMMENT '主键',

    notify_no             varchar(64)  NOT NULL                 COMMENT '通知单号（首次生成：样本编号-yyyyMMddHHmmss；同日重生成不变）',
    notify_type           varchar(20)  NOT NULL DEFAULT 'bioinfo'
                          COMMENT '通知类型（delay 延期通知 / sample 样本不合格 / wet_lab 湿实验不合格 / bioinfo 干实验不合格）',
    notify_status         varchar(20)  NOT NULL DEFAULT 'generated'
                          COMMENT '通知状态（generated 已生成待提交 / pending 待处理 / sent 发送成功 / failed 发送失败）',
    sign_status           varchar(20)  NOT NULL DEFAULT 'none'
                          COMMENT '签署状态（none 未送签 / submitted 已送签 / signed 已签署）',
    notify_date           char(8)      NOT NULL                 COMMENT '通知日期/出具日期（生成当天 yyyyMMdd）',

    subbarcode            varchar(128) NOT NULL                 COMMENT '样本编号（= cms_sample.subbarcode）',
    sample_name           varchar(100)          DEFAULT NULL    COMMENT '样本名称（快照，用于文件命名）',
    sample_file_id        bigint                DEFAULT NULL    COMMENT '样本信息 id（= cms_sample.id）',
    wet_lab_qc_id         bigint                DEFAULT NULL    COMMENT '湿实验质控 id（wet_lab 才有）',
    bioinfo_qc_id         bigint                DEFAULT NULL    COMMENT '生信质控 id（bioinfo 才有）',
    study_no              varchar(128)          DEFAULT NULL    COMMENT '研究方案编号（快照 = cms_sample.scheme_no）',
    project_no            varchar(128)          DEFAULT NULL    COMMENT '项目编号（快照 = cms_sample.project_no）',
    participant_no        varchar(128)          DEFAULT NULL    COMMENT '试验参与者编号（快照 = cms_sample.subject_no）',

    analysis_time         varchar(32)           DEFAULT NULL    COMMENT '分析时间（质控记录入库时间）',
    product               varchar(256)          DEFAULT NULL    COMMENT '检测产品',
    customer_name         varchar(100)          DEFAULT NULL    COMMENT '客户',
    sample_type           varchar(100)          DEFAULT NULL    COMMENT '样本类型',

    qc_stage              varchar(200)          DEFAULT NULL    COMMENT '不合格环节（多选，顿号分隔）',
    terminate_measure     varchar(200)          DEFAULT NULL    COMMENT '是否终止检测（多选，顿号分隔）',
    remedy                varchar(300)          DEFAULT NULL    COMMENT '建议处理办法（多选，顿号分隔）',
    remedy_other          varchar(500)          DEFAULT NULL    COMMENT '建议处理办法-其他（原文）',
    reason                varchar(500)          DEFAULT NULL    COMMENT '不合格原因',
    delay_reason          varchar(500)          DEFAULT NULL    COMMENT '检测报告延迟原因（delay 专用）',
    expected_report_date  varchar(32)           DEFAULT NULL    COMMENT '预计报告出具时间（delay 专用）',

    reporter              varchar(100)          DEFAULT NULL    COMMENT '反馈人',
    report_time           varchar(32)           DEFAULT NULL    COMMENT '反馈时间',
    reviewer              varchar(100)          DEFAULT NULL    COMMENT '复核人',
    review_time           varchar(32)           DEFAULT NULL    COMMENT '复核时间',

    json_path             varchar(500)          DEFAULT NULL    COMMENT '渲染用 JSON 路径（容器内绝对路径）',
    raw_docx_path         varchar(500)          DEFAULT NULL    COMMENT '生成的 DOCX 路径',
    pdf_path              varchar(500)          DEFAULT NULL    COMMENT 'PDF 路径（OnlyOffice 转换产物）',
    signed_pdf_path       varchar(500)          DEFAULT NULL    COMMENT '签名后 PDF 路径（上上签回传落盘）',
    file_path             varchar(500)          DEFAULT NULL    COMMENT '当前交付件路径（生成=PDF；签署完成=签名件；邮件附件取它）',
    sign_contract_id      varchar(64)           DEFAULT NULL    COMMENT '上上签合同号（送签后回填）',

    handler_by            bigint                DEFAULT NULL    COMMENT '处理人 = 生成该通知的人（sys_user.user_id）',
    handler_time          datetime              DEFAULT NULL    COMMENT '处理时间 = 生成时间',
    sender_by             bigint                DEFAULT NULL    COMMENT '发送人（sys_user.user_id）',
    sender_time           datetime              DEFAULT NULL    COMMENT '发送时间',
    send_to               varchar(500)          DEFAULT NULL    COMMENT '实际收件人（发送时快照，;分隔）',
    send_cc               varchar(500)          DEFAULT NULL    COMMENT '实际抄送（发送时快照，;分隔）',
    send_error            varchar(500)          DEFAULT NULL    COMMENT '最近一次发送失败原因（成功时清空）',

    create_by             bigint                DEFAULT NULL    COMMENT '创建者',
    create_time           datetime              DEFAULT NULL    COMMENT '创建时间',
    update_by             bigint                DEFAULT NULL    COMMENT '更新者',
    update_time           datetime              DEFAULT NULL    COMMENT '更新时间',
    del_flag              char(1)      NOT NULL DEFAULT '0'      COMMENT '删除标志（0存在 1删除）',
    tenant_id             varchar(20)  NOT NULL DEFAULT '000000' COMMENT '租户编号',

    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_sample_notify_subbarcode (tenant_id, subbarcode),
    KEY idx_sample_notify_day (tenant_id, subbarcode, notify_type, notify_date),
    KEY idx_sample_notify_status (tenant_id, notify_status, notify_date),
    KEY idx_sample_notify_project (tenant_id, project_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '样本通知台账（延期/样本不合格/湿实验不合格/干实验不合格）';


-- ============================================================================
-- 菜单与按钮：挂在「样本管理」（parent_id=0 AND path='sample'）下第 4 个
--   页面 perms = qc:notify:list；按钮 qc:notify:submit / qc:notify:send
--   (qc:notify:query / generate / sign 已在生信质控菜单下，复用，不重复建)
--   授权由用户自行配置，这里只建点位。
-- ============================================================================
SET @quality_id = (SELECT menu_id FROM sys_menu WHERE parent_id = 0 AND path = 'sample' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT 'route.sample_notify', @quality_id, 4, 'notify', 'sample/notify/index', 1, 0,
    'C', '0', '0', 'qc:notify:list', 'local-icon-email', 1, NOW(), '异常处理（四类通知台账）'
FROM (SELECT 1) AS dummy
WHERE @quality_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM (SELECT menu_id FROM sys_menu
      WHERE parent_id = @quality_id AND path = 'notify') AS exists_check);

UPDATE sys_menu
SET parent_id = @quality_id, menu_name = 'route.sample_notify', component = 'sample/notify/index',
    path = 'notify', order_num = 4, menu_type = 'C', perms = 'qc:notify:list',
    visible = '0', status = '0', update_by = 1, update_time = NOW()
WHERE parent_id = @quality_id AND path = 'notify';

SET @notify_menu_id = (SELECT menu_id FROM sys_menu
    WHERE parent_id = @quality_id AND path = 'notify' LIMIT 1);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT m.menu_name, m.menu_id, m.order_num, '', NULL, 1, 0, 'F', '0', '0', m.perms, '#', 1, NOW(), m.remark
FROM (SELECT @notify_menu_id AS menu_id, '提交' AS menu_name, 1 AS order_num, 'qc:notify:submit' AS perms, '签名完成后提交给发送人' AS remark
      UNION ALL SELECT @notify_menu_id, '发送邮件', 2, 'qc:notify:send', '向客户发送通知邮件') AS m
WHERE m.menu_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM sys_menu x
    WHERE x.parent_id = m.menu_id AND x.perms = m.perms);
