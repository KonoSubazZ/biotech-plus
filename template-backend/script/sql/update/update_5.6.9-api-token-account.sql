-- 固定 token 接口调用身份：sys_user.tumor
-- 为什么：POST /qc/bioinfo/receive 这类内部接口由上游系统调用，不登录换 token ——
--         调用方带固定 token（application.yml 的 api.token.value），服务端按 api.token.account 的身份执行。
--         所以这个账号必须存在、且被授权（权限点位：qc:bioinfo:receive / qc:bioinfo:list / qc:bioinfo:query）。
-- 归属：默认租户 000000（平台身份，跨药企可见）；角色：超级管理员(1) —— 也可按最小权限另行调整。
-- 密码：可登录密码（仅调试用），重置走「系统管理 › 用户」；接口调用只用固定 token，不依赖这个密码。
-- 幂等：按 user_name 判重，已存在则只补角色授权。
-- 体检：python3 tools/check_db_schema.py（本脚本只写数据，不改表结构）

SET NAMES utf8mb4;

SET @tumor_pwd = '$2a$10$Aq3f571YWVm.2niMm0Z3XudTkBQpFmzCyyrFzvQtP5d73XZVlD/rm';

INSERT INTO `sys_user` (`user_name`, `nick_name`, `user_type`, `password`, `status`, `del_flag`,
    `tenant_id`, `create_by`, `create_time`, `remark`)
SELECT 'tumor', '生信质控接口账号', 'sys_user', @tumor_pwd, '0', '0', '000000', 1, NOW(),
    '固定 token 接口调用身份（api.token.account）；密码可重置，接口调用只用固定 token'
FROM (SELECT 1) AS dummy
WHERE NOT EXISTS (SELECT 1 FROM (SELECT user_id FROM `sys_user` WHERE user_name = 'tumor') AS exists_user);

-- 授权超级管理员角色（role_id = 1）；换别的角色/权限点位时改这里
INSERT IGNORE INTO `sys_user_role` (`user_id`, `role_id`)
SELECT user_id, 1 FROM `sys_user` WHERE user_name = 'tumor';

SELECT u.user_id, u.user_name, u.nick_name, u.tenant_id, u.status, GROUP_CONCAT(r.role_key) AS roles
FROM `sys_user` u
LEFT JOIN `sys_user_role` ur ON ur.user_id = u.user_id
LEFT JOIN `sys_role` r ON r.role_id = ur.role_id
WHERE u.user_name = 'tumor'
GROUP BY u.user_id, u.user_name, u.nick_name, u.tenant_id, u.status;
