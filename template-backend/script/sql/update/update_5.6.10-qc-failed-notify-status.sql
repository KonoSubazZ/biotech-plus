-- 样本质控不合格通知：新增通知状态列（已生成（文档）/ 已发送（邮件））
-- 背景：qc_failed_notify 建表时没有状态列，单列迁移避免改已有数据；可重复执行一次。
-- 执行：docker exec -i biotech-plus-mysql mysql -uroot -p*** --default-character-set=utf8mb4 ry-vue < 本文件

SET NAMES utf8mb4;

ALTER TABLE `qc_failed_notify`
    ADD COLUMN `notify_status` varchar(20) NOT NULL DEFAULT '已生成（文档）'
        COMMENT '通知状态：已生成（文档）/ 已发送（邮件）' AFTER `notify_no`;

SELECT id, notify_no, notify_status FROM `qc_failed_notify`;
