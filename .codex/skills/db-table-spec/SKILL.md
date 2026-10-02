---
name: db-table-spec
description: >
  在 biotech-plus（RuoYi-Vue-Plus 多租户 + MySQL 8）里新建表、加字段或写迁移脚本时使用。
  触发场景：要建一张业务表、要给现有表加列/加索引、要把 docs/context/设计文档 的「数据模型」
  章节落地成建表 SQL、"这张表要不要加 tenant_id"、"索引怎么建"、"表名前缀怎么起"、
  插入报 Unknown column 'tenant_id' 或 Field 'xxx' doesn't have a default value。
  给出：表命名前缀（= 所属菜单模块）、公共字段、主键自增策略、索引（idx_tenant_id / uk_* 带租户）、
  字符集排序规则，以及设计文档(PG/Prisma) → MySQL 的类型映射；并附一条命令的体检闸门。
effort: high
---

# db-table-spec — 数据库表规范（建表 / 改表）

## 规则只写一处：本文件是入口，不是正文

| 要什么 | 去哪 |
|---|---|
| 规则正文（为什么 + 每条的判据） | `ai-rules/04-db-schema.md` |
| 可复制 DDL 模板（含复合主键变体） | `ai-templates/db/table-template.sql` |
| 机械闸门（一条命令出报告） | `python3 tools/check_db_schema.py` |
| 系统表基线 / 迁移脚本示例 | `template-backend/script/sql/ry_vue_5.X.sql`、`script/sql/update/update_5.6.2-*.sql` |

改规则时三处一起改：**规则文件 + 模板 + 闸门**。

## 五个必须先记住的硬约束

1. **每张业务表都要 `tenant_id varchar(20) NOT NULL DEFAULT '000000'` + `KEY idx_tenant_id (tenant_id)`**。
   租户拦截器是无条件注册的（`TenantLineHandlerImpl`），漏列 → 该模块读写直接抛 `Unknown column 'tenant_id'`。
2. **单列主键必须 `bigint NOT NULL AUTO_INCREMENT`**（全局 `idType = AUTO`）。漏自增 →
   `Field 'xxx' doesn't have a default value`。复合主键反过来：**不自增**，实体 `@TableId(type = IdType.INPUT)`。
3. **建表必须显式写 `COLLATE=utf8mb4_general_ci`**，不写就是 MySQL 8 默认的 0900，跨表 JOIN 报
   `Illegal mix of collations`。
4. **业务唯一键让 tenant_id 打头**：`UNIQUE KEY uk_<表>_<语义> (tenant_id, <列>)`，否则第二个租户建同编码会失败。
5. **公共字段四件套** `create_by / create_time / update_by / update_time`（**没有 create_dept** —— sys_dept 已删）。
   `del_flag` 只给「记录类」表；日志/审计类（audit_log、dev_log、validation_record、data_archive_record）不加。

## 步骤

1. **定位模块前缀** = 它归属的菜单模块（`qc_` / `product_` / `company_` / `audit_` / `compliance_` /
   `data_` / `sample_` / `dev_` / `validation_`），映射见 `04-db-schema.md` §1。
   不要造 `biz_` / `tmp_` 这类没有模块归属的前缀。
2. **复制模板** → `template-backend/script/sql/business/<模块>.sql`，替换 `<模块>/<实体>/<中文名>`；
   业务字段按 §7 的映射表补（设计文档是 PG/Prisma 写法，`uuid`/`timestamp`/`jsonb` 不能照抄）。
3. **执行**（走文件；不要在 shell 双引号里拼反引号 —— bash 会当命令替换，SQL 静默失败）：
   ```bash
   docker exec -i -e MYSQL_PWD=root biotech-plus-mysql \
     mysql -uroot --default-character-set=utf8mb4 ry-vue < <模块>.sql
   ```
4. **过闸门**：`python3 tools/check_db_schema.py` → error 必须为 0。
5. **建实体**：`python3 tools/new_crud.py --module <模块> --entity <实体> --table <表> ...`（见 `crud-module` skill）。
6. **菜单与权限点位**：`sys_menu` 加菜单，`perms` 与后端 `@SaCheckPermission` 逐字一致（见 `crud-module` skill）。
7. **真机验证一次 CRUD**：插入后看主键是否自增、tenant_id 是否被自动补上。

## 不要做

- 不要省 tenant_id；也不要给白名单表加 tenant_id（除非它是标识列，如 `sys_tenant`）。
- 不要建 `authz_permissions` / `authz_role_permissions`（等价于 `sys_menu.perms` + `sys_role_menu`）、
  不要建 `company`（**就是 `sys_tenant`**，即「药企管理」）。
- 不要直连改已发布的迁移脚本 —— 要改就新加一个 `update_<版本>-<主题>.sql`。
- 不要在 `mysql -e "..."` 里写带反引号的 DDL；别忘了 `SET NAMES utf8mb4;`。

## 验收清单

- [ ] 表名前缀 = 所属菜单模块
- [ ] 公共字段齐全（无 create_dept；日志/审计类无 del_flag）
- [ ] 单列主键 bigint AUTO_INCREMENT（复合主键则不自增 + 实体 INPUT）
- [ ] `tenant_id` 列定义正确 + `idx_tenant_id` 索引
- [ ] 业务唯一键全部以 tenant_id 打头
- [ ] 显式 `COLLATE=utf8mb4_general_ci`
- [ ] `python3 tools/check_db_schema.py` → error 0
- [ ] 真机插入一次：主键自增、tenant_id 自动补
