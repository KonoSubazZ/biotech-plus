-- canal 只读复制账号：Canal 读 binlog 需要 REPLICATION SLAVE/CLIENT，业务侧只需要 SELECT。
-- 依据：.hermes/plans/2026-10-21_164831-canal-cdc-audit-log.md Phase 1
-- 幂等：可重复执行（ai-rules/04-db-schema.md §6）
SET NAMES utf8mb4;

CREATE USER IF NOT EXISTS 'canal'@'%' IDENTIFIED BY 'canal_2026';
CREATE USER IF NOT EXISTS 'canal'@'localhost' IDENTIFIED BY 'canal_2026';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%';
GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'localhost';
FLUSH PRIVILEGES;
