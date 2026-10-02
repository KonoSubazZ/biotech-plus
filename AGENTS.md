# AGENTS.md — Biotech-Plus 仓库 AI 入口

> 本文件是全仓库 AI 的**第一入口**。子目录契约优先于本文件：
> - 改 Java → 先读 `template-backend/AGENTS.md`，细节读 `template-backend/docs/backend-code-standard.md`
> - 改前端 → 先读 `template-frontend/.claude/CLAUDE.md`（`.codex/` 同源），设计约束读 `template-frontend/DESIGN.md` 与 `UX-CONTRACT.md`
> - 本文件只管：仓库是什么、文档在哪、代码在哪、怎么跑、怎么交付、有哪些坑。
> - **写代码 / 提 PR 前必读 `ai-rules/`**（可读性判据、评审契约、多人协作），并跑一次
>   `python3 tools/readability_check.py --changed-only origin/main`。新模块从 `ai-templates/` 复制。
>   页面结构另有闸门：`python3 tools/structure_check.py --changed-only origin/main`（两个都要过）。
>   动数据库表另有闸门：`python3 tools/check_db_schema.py`（建表规范见 `ai-rules/04-db-schema.md`）。
>   **改完必须规范提交**：先跑一次 `bash tools/install-git-hooks.sh` 装钩子，
>   之后每次提交会自动跑闸门并校验提交信息（type/scope/中文简述，规范见 `ai-rules/05-git-commit.md`）。

---

## 1. 这个仓库是什么

Monorepo，两个可独立构建的子项目：

| 目录 | 技术栈 | 说明 |
|---|---|---|
| `template-backend/` | Java 17 · Spring Boot 3.5.16 · Maven · MyBatis-Plus · Sa-Token | RuoYi-Vue-Plus 5.6.2，多租户 |
| `template-frontend/` | Vue 3.5 · TypeScript 5.8 · Vite 6.2 · Naive UI · pnpm | Soybean Admin 风格 |

**目标系统**：药企 NGS 报告运营系统（从样本到报告的全流程闭环 + 合规）。顶层架构见 `docs/context/Project Workflow.png`。

### 1.1 当前状态（最重要的一条）

- **业务代码尚未实现。** 后端只有 RuoYi 自带模块（`ruoyi-system` / `ruoyi-generator` / `ruoyi-job`），前端只有模板页面（`about` / `home` / `monitor` / `system` / `tool`）。
- 仓库现有的内容是**模板脚手架 + 一批设计文档**，两者尚未合并。
- 设计文档描述的业务表（`qc_record`、`product_config`、`audit_log`、`analysis_report` 等 13 张）在全仓代码里**命中 0 次**，也没有对应的建表 SQL。

### 1.2 技术栈错位（必须知道，否则一定写错）

`docs/` 里的设计文档是从**另一套已实现的项目**逆向整理出来的，栈与本仓库不同：

| 文档里写的 | 本仓库实际的 |
|---|---|
| NestJS + TypeScript（`server/modules/qc/`、`QcJudgeHelper`） | Java + Spring Boot（`ruoyi-modules/*/src/main/java/...`） |
| React + shadcn + zod（`client/src/pages/`） | Vue 3 + Naive UI（`template-frontend/src/views/`） |
| PostgreSQL + Prisma（`defaultRandom`、`23505`、`onConflictDoNothing`） | MySQL 默认，MyBatis-Plus（`domain` / `bo` / `vo`） |
| `@Can('read','QcControl')` 装饰器 | `@SaCheckPermission` |
| dataloom SDK 上传文件、飞书 `role_manager`/`ChatSelect` | RuoYi 自带 OSS、`ruoyi-system` 用户/角色 |
| ExcelJS `XLSX.read` | `ruoyi-common-excel`（EasyExcel） |

**结论：把设计文档当「需求输入」，不要当「实现规格」。** 照抄文档里的路径、表类型、权限注解、SDK 会直接写出对不上的代码。
动手前若不确定按哪套栈实现，**先问，不要自己选**。

---

## 2. 文档地图

| 路径 | 内容 | 什么时候读 |
|---|---|---|
| `docs/context/Project Workflow.png` | 顶层系统架构图（6 层）+ 从样本到报告的全流程闭环图（含失败分支） | **开工前必读**，建立全局 |
| `docs/context/设计文档/README.md` | 模块索引、模块间数据关系（mermaid）、权限矩阵汇总 | 找模块、看关系 |
| `docs/context/设计文档/spec.md` | 湿实验质控 —— 最完整的一篇（表结构 / 判定算法 / API / Excel 导入 / 权限） | 写文档或写码的范例 |
| `docs/context/设计文档/<模块>.md` | 其余 11 篇模块设计文档，格式统一为「数据模型 + API + 功能流程 + 角色权限」 | 改对应模块前 |
| `docs/context/流程图/<模块>-flow.md` | 5 条业务流程（wetlab-qc / report / company-access / data-retention / system-management） | 理业务流程 |
| `docs/context/设计文档/task_implement.json` | 文档生成过程记录；其 `write_scope` / `read_scope` 指向外部项目路径，**勿依赖** | 仅追溯来源 |

设计文档里出现的 `classDef unimplemented stroke-dasharray: 5 5` 表示该节点**未实现**，不是笔误。

---

## 3. 代码结构（实际路径）

### 3.1 后端 `template-backend/`

```
ruoyi-admin/            启动模块（入口 org.dromara.DromaraApplication），端口 8080，context-path /
  src/main/resources/   application.yml / application-dev.yml / application-prod.yml
ruoyi-common/           23 个子模块（ruoyi-common-bom 统一版本 + 22 个能力模块：core / web /
                        security / mybatis / redis / oss / excel / satoken / log / idempotent /
                        ratelimiter / sse / websocket / translation / sensitive / job / mail /
                        sms / social / encrypt / doc / json）
ruoyi-modules/
  ruoyi-system/         用户 / 角色 / 菜单 / 部门 / 租户（业务代码放这里或新增同级模块）
  ruoyi-generator/      代码生成器
  ruoyi-job/            定时任务
ruoyi-extend/           monitor-admin（Spring Boot Admin 9090）、snailjob-server（17888）
config/checkstyle/      checkstyle.xml（绑定 Maven validate 阶段）
script/                 bin / docker / sql / tests（含 checkstyle-regression.ps1）/ leave
docs/backend-code-standard.md
```

分层约定（新代码必须遵守）：
`domain`（实体）· `domain/bo`（请求对象）· `domain/vo`（响应对象）·
`XxxController`（路由/校验/权限/封装返回）· `IXxxService` + `XxxServiceImpl`（业务/事务）· `XxxMapper`（持久化）

### 3.2 前端 `template-frontend/`

```
src/views/        页面（当前仅 about / _builtin / home / monitor / system / tool）
src/service/api/  接口层    src/service/request/  请求封装
src/store/        Pinia     src/router/           路由（文件路由，用 pnpm gen-route 生成）
src/components/   组件      src/hooks/            组合式函数
src/typings/      类型      src/theme/            主题（运行时真值在 theme/settings.ts、vars.ts）
src/locales/      国际化    src/enum/ src/constants/
packages/         alova / axios / color / hooks / materials / scripts / uno-preset / utils
docs/             模板代码生成模板（*.vm）+ 建表 SQL
```

---

## 4. 架构图 → 代码落位

`Project Workflow.png` 的左图分 6 层，对照当前代码：

| 架构层 | 图上内容 | 现状 |
|---|---|---|
| 业务入口层 | 项目/录单/实验室/生信/病理/解读 等角色 | 部分复用 `ruoyi-system`（用户、角色、多租户） |
| 业务编排与规则层 | 流程编排引擎、报告配置决策引擎、异常处理、电子签名、通知中心 | **无**，需新建 |
| 核心域服务层 | 录单与项目管理、LIMS 对接、测序下机追踪、生信 QC、变异标准化、知识库、报告生成、交付归档 | **无**，需新建 |
| 数据与集成层 | LIMS DB/API、FASTQ/BAM/VCF、知识库、模板库、规则库、报告 PDF、API Gateway / Event Bus | 部分底座已有（`ruoyi-common-oss` / `redis` / `mybatis`） |
| 合规与验证控制层 | RBAC、审计追踪、电子记录、版本控制、变更控制、备份归档、接口可追溯 | 部分（`@Log`、Sa-Token 权限、`ruoyi-common-log`） |
| CSV / 3Q 验证 | URS、风险评估、IQ/OQ/PQ、RTM、验证报告、上线、变更后回归 | **无** |

右图是「从样本到报告」的主流程闭环，两条失败分支：湿实验失败（记录失败环节 + 失败编码 + 原始证据 → 责任角色填因 → 上上签电子签署 → 运营审核）；生信 QC 不合格（固化证据 → 技术原因 → 双方电子签署 → 交付归档）。规则引擎的输入是「产品编号 + 客户名称 + 项目编号 + 癌种 + 样本类型 + 其他合同字段」，输出「检测模块 + Gene List + 报告模板版本」。

---

## 5. 业务模块与路由（来自设计文档）

| 模块 | 路由 | 文档 |
|---|---|---|
| 湿实验质控 | `/qc/quality-control` | `设计文档/spec.md` |
| 生信质控 | `/qc/bio-qc` | `设计文档/bioinfo-qc.md` |
| 公司管理 | `/company/management` | `设计文档/company-management.md` |
| 权限申请 / 审批 | `/company-access`、`/company-access/approval` | `设计文档/company-access.md` |
| 审计日志 | `/audit/logs` | `设计文档/audit-log.md` |
| 3Q 文档管理 | `/compliance/documents` | `设计文档/compliance-docs.md` |
| 开发记录 | `/compliance/dev-log` | `设计文档/dev-log.md` |
| 3Q 验证记录 | `/compliance/validation-records` | `设计文档/validation-record.md` |
| 产品配置 | `/system/product-config` | `设计文档/product-config.md` |
| 数据周期管理 | `/system/data-retention` | `设计文档/data-retention.md` |
| 系统管理 | `/system/role`、`/system/user`、`/system/menu` | `设计文档/system-management.md` |
| 报告管理 | `/report/management`、`/report/interpretation` | **无设计文档**，仅 `流程图/report-flow.md` |

角色：`wet_lab`（湿实验员）· `bioinformatician`（生信分析师）· `interpreter`（报告解读员）· `operation_admin`（运营管理员）。
权限点位形如 `read:QcControl` / `manage:ProductConfig` / `apply:CompanyAccess` / `approve:CompanyAccess`。

---

## 6. 构建、运行、验证（真实命令）

### 后端（在 `template-backend/` 下执行）

```bash
mvn -version                                    # 先确认是 JDK 17
mvn -B -ntp validate                            # 会连带跑 Checkstyle（绑定在 validate）
mvn -B -ntp -pl ruoyi-modules/ruoyi-system -am validate   # 定向模块
```

- 测试默认被父 POM 跳过，必须显式开：`-DskipTests=false`，且要按 `${profiles.active}` 匹配 Surefire 标签。
- 改过检查规则后跑：`script/tests/checkstyle-regression.ps1`。
- 期望结果：末尾 `BUILD SUCCESS`。**核实实际执行的测试数量，零测试不等于通过。**

### 前端（在 `template-frontend/` 下执行）

```bash
pnpm install
pnpm dev          # vite --mode dev
pnpm typecheck    # vue-tsc --noEmit --skipLibCheck
pnpm lint         # oxlint --fix && eslint --fix .
pnpm gen-route    # 生成路由
```

- Node ≥ 20.19.0，pnpm ≥ 10.5.0。
- 后端地址：`VITE_SERVICE_BASE_URL=http://localhost:8080`，接口前缀 `VITE_APP_BASE_API=/dev-api`。
- 前端状态目录：若存在 `.ai_state/`，**进入决策前先读 `.ai_state/_index.md`**，不要 glob 全目录扫描。

---

## 7. 写代码的硬规则

以下继承自两份子规范，这里只列最容易违反的：

**Java**
- 分层不得越界：Controller 不写业务逻辑，Service 不拼 SQL，Mapper 不做业务判断。
- 单表 CRUD 用 `BaseMapperPlus` + Lambda Wrapper；复杂联表 / 子查询 / 聚合报表写 Mapper XML。
- **禁止**用 `@Select` / `@Insert` / `@Update` / `@Delete` / `*Provider` / `@SelectKey` 在 Java 里写 SQL（Checkstyle 会拦）。
- SQL 值参数用 `#{param}`；动态表名/列名/排序字段必须服务端白名单校验。
- 优先级：可读性 > 代码行数。允许多写有业务含义的局部变量和步骤，不为了少几行引入难懂的抽象。
- **不顺手重构**：只改本次需求涉及的方法，不动未涉及的历史代码。

**前端**
- 复用 Naive UI 组件与既有 hooks（`useNaivePaginatedTable`、`useTableOperate`、`useNaiveForm`）；不新引 UI 库。
- 单选用 `NSelect`，提示用应用级 `window.$message`，滚动条由全局样式与 `scrollbar.scss` 拥有。
- 类型放 `src/typings/`，不要就地 `any`。
- 视觉改动遵守 `DESIGN.md` 的令牌与 `UX-CONTRACT.md` 的能力归属表；不新增页面级阴影/主题色。

**通用**
- 不为「将来可能的需求」预留抽象；没有第二个实现就不要接口/工厂/策略。
- 不新增第三方依赖，除非在交付说明里给出理由。
- 不硬编码密钥、口令、Token、IP；从环境或配置读取。
- **不编造**：任何路径、字段名、接口、配置项都必须来自你实际读过的文件；不确定就说不知道。

---

## 8. 交付格式

每次汇报按四段写，缺一不可：

1. **改了什么** —— 文件清单 + 每个文件做了什么
2. **验证了什么** —— 实际执行的命令
3. **真实终端输出** —— 原样粘贴，不许编造；未运行或受阻必须明说
4. **结论** —— 完成 / 未完成 / 遗留问题

写脚本或函数时另附：预期输出（含 2~3 组边界输入）、自测用例（`assert` + PASS/FAIL 打印）、真实运行结果。

---

## 9. 已知缺口（别踩，也别假装它们不存在）

- **没有业务表 DDL**：设计文档只有 markdown 字段表，全仓无对应建表 SQL。要建表先确认口径，不要凭文档猜类型。
- **报告模块没有设计文档**：只有 `流程图/report-flow.md`，且该文档自列 4 项未实现（报告预览 / 审核 / 报告发送 / 归档的实际搬迁操作）。
- **没有 API 契约文件**：无 OpenAPI/Swagger/共享 TS 类型，接口以设计文档的表格为准（40+ 个路径）。
- **权限点位不全**：`system-management.md` 称「现有 20 个权限点位」，而 `README.md` 权限矩阵只列了 13 个，缺 7 个无据可查。
- **文档存在死链**：`设计文档/README.md` 的流程图表链接指向 `../../docs/flowchart/...`，实际目录是 `docs/context/流程图/`，5 处同类引用全部失效。
- **用户管理 / 菜单管理是纯前端 demo**：读本地模拟数据（`initialUsers` / `initialMenus`），无后端 API，修改不持久化（见 `system-management.md` 第 6 节）。
- **`sample_qc_info` 字段未穷举**：文档写「另有 20+ 项指标」，缺完整清单。
- **本仓库没有根 README**：本文件即入口；两个子项目的 README 分别是 RuoYi 和 Soybean 模板自带的，不是本系统的说明。

---

## 10. 不确定时

按此顺序处理，不要跳步：

1. 先读上面第 2 节的文档地图对应那一篇
2. 再读对应目录的代码（`app`/`src` 里找同类既有实现，照它写）
3. 仍然不确定 → **停下来问**：按哪套栈实现、表结构以哪份为准、权限点位怎么定

禁止在没有依据的情况下自行选择技术栈或凭空补齐缺失的规范。

---

## 11. AI 协作规则与闸门（写代码 / 提 PR 前必读）

本仓库有一层**跨前后端、跨工具（Hermes / Codex / Claude）共用**的规则，只写一处：

| 路径 | 作用 |
|---|---|
| `ai-rules/README.md` | 规则总览 + 三条命令 |
| `ai-rules/01-readability.md` | **可读性契约**：禁语法糖、禁过度设计、尺寸红线、错误处理、反例正例 |
| `ai-rules/02-review-contract.md` | **评审契约**：写与审分离、交接内容、输出格式、轮次与裁决、与 Athena 的衔接 |
| `ai-rules/03-collaboration.md` | **多人协作**：接口先行、模块 owner、热点文件串行、分支与 CI 闸门 |
| `ai-rules/04-db-schema.md` | **数据库表规范**：表命名前缀、公共字段、索引、租户字段、主键自增、字符集排序规则、PG→MySQL 映射 |
| `ai-rules/05-git-commit.md` | **提交规范**：type/scope 白名单、subject 规则、提交前四步、钩子安装 |
| `tools/readability_check.py` | **机械闸门**：Java + Vue/TS 一条命令出报告 |
| `tools/structure_check.py` | **结构闸门**：页面落位 / import 目标 / 统一 hooks / 类型位置 / 跨端权限点位 |
| `tools/check_db_schema.py` | **数据库闸门**：租户列/索引/主键自增/公共字段/排序规则/白名单一致性 一条命令出报告 |
| `tools/check_commit.py` | **提交信息闸门**：type/scope/subject 格式校验（commit-msg 钩子调用，也可手动跑） |
| `tools/install-git-hooks.sh` | 一条命令装 git 钩子（pre-commit 三道闸门 + commit-msg 信息校验） |
| `tools/new_crud.py` | **CRUD 脚手架**：一条命令生成后端模块 + 前端页面，并注册 Maven 模块 |
| `ai-templates/backend-module/` | 后端新模块骨架（Controller/Service/Mapper/Bo/Vo/XML） |
| `ai-templates/frontend-module/` | 前端新页面骨架（index.vue / search.vue / operate-drawer.vue / api.ts / types.d.ts） |
| `ai-templates/db/table-template.sql` | 建表 SQL 模板（公共字段/索引/租户列骨架 + 复合主键变体） |
| `.claude/skills/crud-module/` | CRUD 生成 skill（`.codex/skills/` 同源） |
| `template-frontend/.claude/skills/` | `readability-guard`（写完自检）、`cross-review`（交叉评审） |

### 常用命令（提 PR 前两道闸门必跑）

```bash
python3 tools/readability_check.py --changed-only origin/main   # 我这次改的合不合规（提 PR 前必跑）
python3 tools/readability_check.py --rules                      # 规则清单
python3 tools/structure_check.py --changed-only origin/main     # 页面结构契约（缺 modules 组件 / 未用统一 hooks / 类型写错位置）
python3 tools/structure_check.py --rules                        # 结构规则清单
python3 tools/check_db_schema.py                                # 数据库表规范体检（建表/改表后必跑，error 必须为 0）
python3 tools/check_db_schema.py --rules                        # 数据库规则清单
python3 tools/check_commit.py --latest 5                        # 校验最近 5 条提交信息（改完要提交时）
python3 tools/check_commit.py --rules                           # 提交规范清单
bash tools/install-git-hooks.sh                                 # 装 git 钩子（每个 clone 装一次即可）
python3 tools/new_crud.py --module <模块> --entity <实体> --table <表> --title <中文> --fields "..." --dry-run
git diff origin/main...HEAD > /tmp/change.diff                  # 交给评审者
```

### 五条不可协商

1. **规则只写一处。** 有争议 → 补进 `ai-rules/`（能机械判定的一并加进闸门），不许留成口头约定。
2. **写的人不审自己的代码。** 人和 agent 都适用；同一分支同一时刻只有一个写者。
3. **未运行 = 未完成。** 报告完成必须附真实终端输出。
4. **不编造事实。** 尤其不许照抄设计文档里的栈（见本文第 1.2 节）。
5. **不为将来预留抽象。** 没有第二个实现就不要接口/工厂/策略。

### 与已有 Athena 机制的关系

`template-frontend/.claude/`、`.codex/` 里的 VibeCoding Athena v9.6（PACE / 9 铁律 / generator·
evaluator·reviewer 子代理 / delivery-gate hook）**继续有效**，`ai-rules/` 是它缺的那三块补充：
可读性判据、机械闸门、跨工具交接契约。两者不冲突，衔接方式写在 `ai-rules/02-review-contract.md` §7。
`template-frontend/.claude/skills/readability-guard/` 与 `cross-review/` 是这套规则的 skill 入口。
