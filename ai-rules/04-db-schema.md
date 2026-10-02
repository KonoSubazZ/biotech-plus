# 04 · 数据库表规范（建表 / 公共字段 / 索引 / 租户字段）

> 适用：`template-backend` 的 MySQL 8 库（本地库名 `ry-vue`）。
> 读者：建表的人、写迁移脚本的人、让 AI 生成 CRUD 的人、Review 的人。
> 机械闸门：`python3 tools/check_db_schema.py`（一条命令出报告，不通过退出码 1）。
> 可复制模板：`ai-templates/db/table-template.sql`。
> 三者关系：**本文件是规则，模板是规则的可执行版本，闸门是规则的检查器** —— 改规则时三处一起改。

---

## 0. 为什么要有这个规范（三条实测背景）

1. **租户是强制的**。`TenantLineHandlerImpl` 把 MyBatis-Plus 的租户拦截器**无条件**注册，规则是
   「除白名单外，所有表都按 tenant_id 过滤」。业务表漏了 tenant_id 列 → 该模块读写直接抛
   `Unknown column 'tenant_id'`（fail-fast，不会静默串租户数据，但功能立刻不可用）。
2. **主键策略是全局的**。`mybatis-plus.global-config.dbConfig.idType = AUTO`
   （`ruoyi-common-mybatis/src/main/resources/common-mybatis.yml` + `ruoyi-admin/.../application.yml`），
   所以**每张表的主键列都必须是 AUTO_INCREMENT**，否则 INSERT 报
   `Field 'xxx' doesn't have a default value`。
3. **字符集/排序规则不统一会炸在 JOIN 上**。实测过一次：`sys_tenant` 是 `utf8mb4_0900_ai_ci`、
   其余 17 张是 `utf8mb4_general_ci`（建表只写了 `DEFAULT CHARSET=utf8mb4`，MySQL 8 就给了 0900），
   跨表 JOIN 报 `Illegal mix of collations`。**建表必须显式写 COLLATE**。

---

## 1. 表命名：前缀 = 它归属的菜单模块

规则：**表名前缀必须能对上它所服务的菜单/路由模块**（照 `sys_` ↔ 「系统管理」、`gen_` ↔ 「系统工具>代码生成」的既有惯例）。

已定（业务模块开发时按此扩展，模块与设计文档的路由一一对应）：

| 菜单模块 | 路由 | 表前缀 | 表 |
|---|---|---|---|
| 湿实验质控 | `/qc/quality-control` | `qc_` | qc_record、qc_standard |
| 报告/样本 | `/report` | `sample_` | sample_file、sample_qc_info |
| 产品配置 | `/system/product-config` | `product_` | product_config |
| 公司管理 | `/company/management` | `company_` | company_access_application（**company 与 sys_tenant 是同一个东西，不另建表**） |
| 审计日志 | `/audit/logs` | `audit_` | audit_log |
| 合规文档 | `/compliance/documents` | `compliance_` | compliance_document |
| 开发记录 | `/compliance/dev-log` | `dev_` | dev_log |
| 3Q 验证记录 | `/compliance/validation-records` | `validation_` | validation_record |
| 数据周期 | `/system/data-retention` | `data_` | data_retention_policy、data_archive_record |

禁止：`biz_` / `tmp_` / `test_` 这类没有模块归属的前缀；驼峰、中文、复数（统一小写单数 snake_case）。

---

## 2. 公共字段（每张业务表必备）

| 字段 | 类型 | 约束 | 注释 | 谁负责写 |
|---|---|---|---|---|
| `create_by` | bigint | NULL | 创建者 | MyBatis-Plus 自动填充（BaseEntity `@TableField(fill = INSERT)`） |
| `create_time` | datetime | NULL | 创建时间 | 同上 |
| `update_by` | bigint | NULL | 更新者 | 同上（`fill = INSERT_UPDATE`） |
| `update_time` | datetime | NULL | 更新时间 | 同上 |
| `tenant_id` | varchar(20) | **NOT NULL DEFAULT '000000'** | 租户编号 | 租户拦截器在 INSERT 时自动补，不依赖实体字段 |
| `del_flag` | char(1) | NOT NULL DEFAULT '0' | 删除标志（0代表存在 1代表删除） | 见下方「谁加 del_flag」 |

**不要 `create_dept`**：`sys_dept` 已在 `update_5.6.2-rbac-only.sql` 里删除，部门维度由租户替代。

**谁加 del_flag**（约定）：
- 加：业务记录类 —— qc_record、sample_file、company_access_application、compliance_document、product_config …
  （用户能"删除"、且删除后可能需要追溯/恢复）
- 不加：日志/审计类 —— audit_log、dev_log、validation_record、data_archive_record
  （append-only，合规上要求不可删改；表只增不删，靠保留策略归档）

---

## 3. 主键：单列 bigint + AUTO_INCREMENT

```sql
id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
PRIMARY KEY (id),
```

- 列名统一用 `id`（现有系统表有的叫 `user_id`/`menu_id`，那是历史，新表不再沿用）。
- 全局 `idType = AUTO` → 实体**不需要** `@TableId(type = IdType.AUTO)`（全局已生效），
  但**表的主键列必须 AUTO_INCREMENT**，这是硬约束。
- **复合主键表**（如 sys_user_role / sys_role_menu）：主键列**不设自增**，实体必须
  `@TableId(type = IdType.INPUT)` 显式传值 —— 新建多对多关系表照这个来。
- 历史提醒：改造前运行期插入的数据是**19 位雪花 ID**（如 sys_menu 里曾有一条
  2106018799459790850）。全库已于 2026-10-02 改为自增并规整，新数据不会再出现雪花值。

---

## 4. 索引设计

```sql
PRIMARY KEY (id),
KEY idx_tenant_id (tenant_id),                        -- 隔离表必备
UNIQUE KEY uk_<表名>_<语义> (tenant_id, <业务列>),      -- 业务唯一键
KEY idx_<业务列> (<业务列>),                            -- 按查询需要
```

规则：
1. **每张隔离表必须有 `idx_tenant_id`**（命名固定，与既有 7 张表一致）。租户条件会出现在几乎每条 SQL 上。
2. **业务唯一键必须让 tenant_id 打头**：`UNIQUE KEY uk_x_y (tenant_id, code)`。
   否则 A 租户占用了 `code=ABC`，B 租户就建不了同编码 —— 这在多租户下是错的。
3. 命名：普通索引 `idx_<列>`、唯一键 `uk_<表>_<语义>`。不要用自动生成的名字（`code`、`code_2`）。
   反例（现存）：`sys_dict_type` 曾有一个自动命名的残留索引 `tenant_id(tenant_id, dict_type)`，已清理。
4. 索引列顺序：**tenant_id 在前，业务列在后**（租户过滤总是第一个条件）。
5. 不给低基数列单独建索引（status / del_flag 只有两个值，单独建索引没意义）。
6. 软删除 + 唯一键的取舍：RuoYi 的做法是唯一键**不含 del_flag**（删除后编码仍被占用）。
   要有"删除后可复用编码"的需求，再单独讨论，不要默默加。

---

## 5. 字符集与排序规则

```sql
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='<中文名>';
```

**COLLATE 必须显式写**。不写 = MySQL 8 默认 `utf8mb4_0900_ai_ci`，与既有 18 张表不一致 →
一旦与外键/关联表 JOIN 就报 `Illegal mix of collations`。

---

## 6. 建表 SQL 放哪、怎么演进

| 场景 | 位置 | 要求 |
|---|---|---|
| 业务表建表 | `template-backend/script/sql/business/<模块>.sql` | 一个模块一个文件；文件头写清明细与依据（设计文档章节） |
| 结构变更/迁移 | `template-backend/script/sql/update/update_<版本>-<主题>.sql` | **幂等**（`IF NOT EXISTS` / 先查 `information_schema` 再 ALTER），文件头写"为什么" |
| 首次部署初始化 | `~/docker/biotech-plus/mysql/init/0N-*.sql`（仓库外） | 由 `deploy.sh up` 从上面两处 cp 刷新 |

- 迁移脚本**只增不改**：已发布过的脚本不要再改内容（别人可能已经跑过），要改就新加一个文件。
- 每个脚本开头 `SET NAMES utf8mb4;`（否则 docker 里 mysql 客户端按 latin1 解析中文，导入中断）。
- 参考已落地的两份：`update_5.6.2-menu-icons.sql`（菜单图标前缀）、
  `update_5.6.2-tenant-version.sql`（租户表 + 给所有业务表补 tenant_id 的存储过程）。

---

## 7. 从设计文档（PostgreSQL/Prisma）翻译到本项目

设计文档（`docs/context/设计文档/*.md`）是**另一套栈**的产物，照抄会写错。映射表：

| 文档里写的 | 本项目 |
|---|---|
| `id uuid PK defaultRandom` | `id bigint NOT NULL AUTO_INCREMENT`（见 §3） |
| `_created_at` / `_created_by` | `create_time` / `create_by` |
| `_updated_at` / `_updated_by` | `update_time` / `update_by` |
| `timestamp` | `datetime` |
| `jsonb` | `json` |
| `boolean` | `char(1)`（'0'/'1'，与既有表一致） |
| `integer` | `int`；`bigint` 保持 `bigint` |
| `text` | `text`（要建索引或长度受限则改 `varchar(n)`） |
| `varchar(n)` | 原样 |
| 通篇没有租户概念 | **每张业务表都要加 `tenant_id`**（见 §2） |
| NestJS 的 `authz_permissions` / `authz_role_permissions` | **不建**：权限点位 = `sys_menu.perms`，角色授权 = `sys_role_menu` |
| NestJS 的 `company` | **不建**：就是 `sys_tenant`（「药企管理」） |

---

## 8. 机械闸门

```bash
# 本机默认（走 docker 里的 mysql 容器）
python3 tools/check_db_schema.py

# 指定库/连接
python3 tools/check_db_schema.py --database ry-vue
python3 tools/check_db_schema.py --rules          # 打印规则清单
python3 tools/check_db_schema.py --json out.json  # 机器可读
```

检查项（error = 必须改，warn = 需说明）：

- `missing-tenant-column`   隔离表缺 tenant_id 列 / 类型不是 varchar(20) / 不是 NOT NULL DEFAULT '000000'
- `missing-tenant-index`    隔离表缺 idx_tenant_id 索引（或列的索引前缀不对）
- `missing-audit-column`    缺 create_by / create_time / update_by / update_time
- `bad-pk`                  单列主键不是 bigint，或（非复合主键表）不是 AUTO_INCREMENT
- `bad-collation`           排序规则不是 utf8mb4_general_ci
- `bad-index-name`          索引/唯一键命名不符合 idx_/uk_ 约定（MySQL 自动命名）
- `bad-del-flag`            del_flag 类型/默认值不对
- `shared-table-has-tenant` 白名单表（共享表）却带了 tenant_id —— 与 `TenantLineHandlerImpl.SHARED_TABLES` 冲突
- `isolated-table-not-scoped` 非白名单表却没有 tenant_id（同 missing-tenant-column，按白名单视角报）

---

## 9. Pitfalls（全部实测踩过）

1. **建表漏 COLLATE** → 与外键/关联表 JOIN 报 `Illegal mix of collations`（§5）。
2. **漏 tenant_id** → `Unknown column 'tenant_id' in 'field list'`，整个模块不可用（§0.1）。
3. **主键没 AUTO_INCREMENT** → `Field 'xxx' doesn't have a default value`（§0.2）。
4. **在 shell 双引号里写反引号** → bash 把 `` ` `` 当命令替换，SQL 被吃掉一半且**静默失败**。
   跑 DDL 一律走文件：`mysql ... < /tmp/x.sql`，别用 `mysql -e "..."` 拼反引号。
5. **docker 里 mysql 客户端默认 latin1** → 中文报 `Data too long for column`，导入中断只剩前几张表。
   导入前 `SET NAMES utf8mb4;` 或加 `--default-character-set=utf8mb4`。
6. **唯一键不带 tenant_id** → 第二个租户建同编码失败（§4.2）。
7. **改索引/列定义用 `ALTER ... MODIFY` 时必须写全列定义**（类型 + NOT NULL + DEFAULT + COMMENT），
   否则注释和默认值会被抹掉。
8. **幂等**：迁移脚本会被重复执行（重建库、回滚重跑），凡是能重复的动作都要 `IF NOT EXISTS` / 先判断。

---

## 10. 交付与验证（新表上线前逐条过）

1. 建表 SQL 落在 §6 规定的位置，文件头写明"为什么"。
2. 跑 `python3 tools/check_db_schema.py` → error 0。
3. 同步建实体：Mapper / Bo / Vo / Controller / Service（`tools/new_crud.py` 可生成骨架）。
4. 权限点位与菜单：`sys_menu` 加菜单 + `perms`（与 `@SaCheckPermission` 逐字一致），
   角色授权走 `sys_role_menu`。
5. 真机验证一次 CRUD（插入看主键是否自增、看 tenant_id 是否被自动补上）。
6. 交付说明按四段写（改了什么 / 验证了什么 / 真实输出 / 结论）。
