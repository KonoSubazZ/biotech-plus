# AGENTS.md — Biotech-Plus 仓库 AI 入口

> 本文件是全仓库 AI 的**第一入口**。子目录契约优先于本文件：
> - 改 Java → 先读 `template-backend/AGENTS.md`，细节读 `template-backend/docs/backend-code-standard.md`
> - 改前端 → 先读 `template-frontend/.claude/CLAUDE.md`（`.codex/` 同源），设计约束读 `template-frontend/DESIGN.md` 与 `UX-CONTRACT.md`
> - 本文件只管：仓库是什么、文档在哪、代码在哪、怎么跑、怎么交付、有哪些坑。
> - **写代码 / 提 PR 前必读 `ai-rules/`**（可读性判据、评审契约、多人协作），并跑一次
>   `python3 tools/readability_check.py --changed-only origin/master`。新模块从 `ai-templates/` 复制。
>   页面结构另有闸门：`python3 tools/structure_check.py --changed-only origin/master`（两个都要过）。
>   动数据库表另有闸门：`python3 tools/check_db_schema.py`（建表规范见 `ai-rules/04-db-schema.md`）。
>   **改完必须规范提交**：先跑一次 `bash tools/install-git-hooks.sh` 装钩子，
>   之后每次提交会自动跑闸门并校验提交信息（type/scope/中文简述，规范见 `ai-rules/05-git-commit.md`）。
> - **分支基线**：本仓默认分支是 `master`，**没有 `main`**（`origin/main` 不存在，闸门会直接报错）。
>   当前主干开发在 `feat/audit-cdc`（比 `master` 多 27 个提交，截至 2026-10-21），
>   所有闸门/评审命令的基线统一写 `origin/master`。

---

## 1. 这个仓库是什么

Monorepo，两个可独立构建的子项目：

| 目录 | 技术栈 | 说明 |
|---|---|---|
| `template-backend/` | Java 17 · Spring Boot 3.5.16 · Maven · MyBatis-Plus · Sa-Token | RuoYi-Vue-Plus 5.6.2，多租户 |
| `template-frontend/` | Vue 3.5 · TypeScript 5.8 · Vite 6.2 · Naive UI · pnpm | Soybean Admin 风格 |

**目标系统**：药企 NGS 报告运营系统（从样本到报告的全流程闭环 + 合规）。顶层架构见 `docs/context/Project Workflow.png`。

### 1.1 当前状态（最重要的一条）

- **业务代码已经实现，不在 `master` 上**：主干开发分支是 `feat/audit-cdc`（比 `master` 多 27 个提交，
  截至 2026-10-21）。本文档旧版本写的"业务代码尚未实现"已经过期，**不要再按那句话从零新建模块**。
- 后端 6 个业务模块（与 RuoYi 自带的 system/generator/job 并存）：
  `ruoyi-project`(18 类) · `ruoyi-qc`(38) · `ruoyi-report`(137) · `ruoyi-compliance`(24) ·
  `ruoyi-audit`(5) · `ruoyi-drive`(29)。
- 报告域 2026-10-27 起的新口径（制品目录按 analysis_date、数字状态码、生信认领、62 只读、历史改到审核通过才冻结）
  集中在 §3.3，权威细节在 `docs/context/report/report-module-design.md` §0.7。
- 前端已实现页面：`report/`（解读 9 页签 / 样本流转 / 样本信息 / 模板 / 邮件配置 / 位点历史）、
  `qc/`（湿实验质控、生信质控）、`project/`（产品配置、质控标准）、
  `compliance/`（3Q 文档、开发记录、验证记录）。
- 业务表 DDL 是有的：`template-backend/script/sql/business/*.sql`（15 个）+ `script/sql/update/*.sql`（43 个迁移）。
- 报告模块的设计与流程文档在 `docs/context/report/`（`report-module-design.md`、`report-module-flow.md`、
  `report-driver/` = 旧 Python 驱动仓，只读参考）。

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
| `docs/context/report/report-module-design.md` | 报告域实现基线；**§0 是权威快照**，§0.7 = 状态码/认领/只读/冻结时机/驳回（2026-10-27） | 改报告域任何一处之前 |
| `docs/context/report/report-module-flow.md`、`report-docx-renderer.md` | 流程图（旧枚举图另有数字码注释）、docx 渲染与**制品目录** | 改流程 / 改产物落盘 |
| `docs/deliverables/`（gitignore） | 甲方交付文档（功能设计 / 数据库设计 / 测试用例 / SOP）。**由 `/home/liushangzhi/project/qa/biotech-plus/gen_docs_*.py` 生成**，手改会被覆盖；截图由同目录 `capture_interpretation.py` 采集 | 改交付文档口径时改生成器再重跑 |

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
  ruoyi-system/         用户 / 角色 / 菜单 / 租户 / 字典 / 客户端（RuoYi 自带）
  ruoyi-generator/      代码生成器（自带）
  ruoyi-job/            定时任务（自带）
  ruoyi-project/        产品配置 / 基因面板 / 质控标准（对应前端 /project/*）
  ruoyi-qc/             湿实验质控 + 生信质控（对应前端 /qc/*，有上游接收接口）
  ruoyi-report/         报告全流程：解读 9 页签 / 样本流转 / 样本信息 / 模板 / 邮件 / 位点历史 /
                        报告生成渲染 / 复审与版本 / 上上签签名（137 个类，最重的模块）
  ruoyi-drive/          数据驱动入库：解析上传文件 → analysis_data / analysis_file_status / file_* 明细
                        （4 个接口，见 §3.2；**不依赖 ruoyi-report**，依赖方向只能 report → drive）
  ruoyi-compliance/     3Q 文档 / 开发记录 / 验证记录
  ruoyi-audit/          审计日志（Canal CDC 入库）
ruoyi-extend/           monitor-admin（Spring Boot Admin 9090）、snailjob-server（17888）
config/checkstyle/      checkstyle.xml（绑定 Maven validate 阶段）
script/                 bin / docker / sql / tests（含 checkstyle-regression.ps1）/ leave
docs/backend-code-standard.md
```

分层约定（新代码必须遵守）：
`domain`（实体）· `domain/bo`（请求对象）· `domain/vo`（响应对象）·
`XxxController`（路由/校验/权限/封装返回）· `IXxxService` + `XxxServiceImpl`（业务/事务）· `XxxMapper`（持久化）

### 3.2 驱动入库 `ruoyi-drive`（2026-10-21 落地，改这块前必读）

**这条链路已经全部在 Java 里了，不要再去找 / 调用 `report-driver/scanner.py`。**
Python 驱动仓（`docs/context/report/report-driver/`，独立 git）现在只作为口径参考与新增类型的解析样板。

- 入口（2026-10-22 收敛为两个）：`POST /report/drive/sample`（`analysisBatchDate`(14) + `productBio` + `subbarcode`
  + `type=UPLOAD|REINTERPRET`）与 `POST /report/drive/file`（`analysisId`+`fileId`），**同步**返回最终状态与文件数。
  外部调用走固定 token `X-Api-Token`（`api.token.paths` 含 `/report/drive/**`），且**必须同时带 `clientid` 头**（缺→401）。
- 文件类型：`utils/DriveFileType` 枚举是唯一权威（第一版只有 1238 产品的 10 个固定文件）；
  文件名规则 `<样本编号>T<suffix>`，枚举顺序即匹配顺序（`.G.qc.txt` 必须排在 `.qc.txt` 前）。
  新增类型 = 加一个枚举值 + 一个 `DriveFileParser` 实现，不改 Registry / Controller。
- 两种驱动逻辑：
  · `type=UPLOAD`（生信上传）→ 三件套（`analysis_batch_date`+`product_bio`+`subbarcode`）已有记录就**拒绝**，
    没有则新建 `analysis_id`（`INSERT … WHERE NOT EXISTS` 原子判重）
  · `type=REINTERPRET`（重新解读）→ 同三件套不判重，**新建 analysis_id**
  · 重新驱动 → **同一批次**：该文件 `del_flag='0'` 的行置 `'1'`，再插一条 `del_flag='0'` 的新行（**新 file_id**）
- **上传幂等（用户 2026-10-22 定）**：同一批次目录重复上传直接拒（返回「该批次已驱动过，拒绝重复上传」），
  不再「每次都新建批次」。失败样本的修复入口是 `/report/drive/file` 单文件重驱；
  同一批次目录要再跑一遍必须用 `type=REINTERPRET`（页面「重新解读」）。
- 样本状态：本次 10 个文件全成功才 `drive_status=11`；缺文件或有失败一律 `10`
  （旧脚本"缺文件仍可能算成功"的口径已废弃）。
- 租户：驱动是平台动作，**不写 tenant_id**（走库默认 000000），数据访问统一包在 `TenantContext.withoutTenant`。
  用户 2026-10-21 定：本期不考虑租户，后续整体移除租户数据权限模块。
- 表约束（改表前必看）：`analysis_data` **没有唯一键**，只有 `idx_analysis_batch (analysis_batch_date, product_bio, subbarcode)`；
  `analysis_file_status`（2026-10-22 由 `data_file_status` 改名）**也没有唯一键**，只有 `idx_file_analysis (analysis_id, file_name)`。
  「重新解读要同三件套多行」「同一文件多历史行」都是设计要求，**不要按旧口径把唯一键加回来**。
  迁移脚本：`5.6.31`（首版 batch_no 方案）→ `5.6.32`（批次口径）→ `5.6.33`（文件状态表改名）→ `5.6.34`（报告表 project_no/product_bio）。
- 未实现：旧驱动其余 30 个文件类型（tmb / her2 / mgmt / immune_all / neoantigen / mrd / pdinfo …）
  本轮按用户口径不做，且这些老明细表在本地库也不存在；等新驱动逻辑口径确定后再加枚举 + 解析器。
- 按钮接线：`InterpretationServiceImpl.redriveFile`、`ReportReinterpretService.reinterpret` 只调 `IDriveService`，
  不要在 report 模块里再写解析/落库。
- 配置位置：`drive.root-dir` **按环境放在 profile 文件**——`application-dev.yml` = `/tmp/rp-upload`，
  `application-prod.yml` = `${DRIVE_ROOT_DIR:}`（留空时调用即快速失败，不猜生产路径）；
  共用 `application.yml` 只放与环境无关的项（create-by / operator-name / 批量行数 / file_text 上限）。
  生产必须把该目录挂进后端容器，`deploy.sh` 的 `DRIVER_UPLOAD_DIR` 要与之一致。

### 3.3 报告制品目录与状态机（report 域，2026-10-27 口径，改这块前必读）

**制品根** = `report.artifact.root`（容器 `/app/reports`，宿主 `/home/liushangzhi/project/reports`），
日期目录**统一取 `analysis_report.analysis_date`**（不是生成当天），空或非 8 位数字直接报错、不猜：

```text
report/{json,docx,pdf,signed}/<yyyyMMdd>/   四个阶段，同一轮共用一个文件名前缀
report/rejected/<yyyyMMdd>/<subbarcode>_V<n>/{json,docx,pdf,signed}/ + manifest.json
.work/<tenant>_<reportId>_<轮次>/           临时区，成功后逐阶段 move，失败直接丢弃
```

- 文件名 = `客户_样本_模板名_V<版本>`；同 reportId+version+analysis_date 重生成 → **同名覆盖**；
  只有「发送成功」（sent_version+1）之后版本号才递增。不再有 `report/<tenant>/<reportId>/V<n>/current` 那一层。
- 生成失败：只丢临时区（不建 `failed/`、不留半成品、不落库），状态落 33。
- 审核驳回：先把制品 move 到 `report/rejected/...`（+ `manifest.json`），再 `clearReportArtifact`
  把库里 4 个路径字段置空，状态回退 31；归档失败则抛错、状态不变。

**状态机**（权威定义 `utils/ReportStatus.java`，数字码，不写英文枚举）：

```text
20 生信看点 → 21 生信审核 → 30 报告待解读 → 31 报告解读中 →（32 生成成功 / 33 生成失败）
  → 40 报告待审核 → 41 报告审核中 →（43 审核成功，瞬时）→ 50 签名中 → 60 报告待发送
  →（61 发送失败 / 62 发送成功终态）；40/41 驳回 → 归档后回退 31
```

- 42（审核失败）、53（签名成功）**只是定义了、代码不落库**：驳回直接回 31，签名件到手直接写 60。
- 生信认领（2026-10-27，方案 B，未加列）：点「处理」把 `report_bioinfo_viewed_by` / `report_bioinfo_reviewed_by`
  置为当前用户（`WHERE report_status=20/21 AND (字段 IS NULL OR 字段=当前用户)`，0 行 → 「已被他人认领」），
  点「完成」再校验认领人并推进状态；认领不是审核的硬前置，也不自动释放。
- 62 报告的「查看解读」走 `POST /report/interpretation/enter` 的 `readonly=true`：只返回该批次已有报告，
  不新建/不补快照/不校验流转状态；前端详情只读（`pointer-events:none` + 置灰），「重新解读」才是重扫出新批次。
- **位点历史只在「审核通过」时冻结**：提交审核（32→40/41）不写 `history_somatic/history_germline`、
  不写 `report_variant_history`、不登记 `history_reuse_log`；驳回也不写。详细口径与代码落点见
  `docs/context/report/report-module-design.md` §0.7。

### 3.4 前端 `template-frontend/`

```
src/views/        页面（about / _builtin / home / monitor / system / tool + 业务页 report / qc / project / compliance）
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
| 产品配置 | `/project/product-config`（**不是** `/system/product-config`） | `设计文档/product-config.md` |
| 质控标准 | `/project/qc-standard` | 实现时新增，设计文档未单列 |
| 数据周期管理 | `/system/data-retention` | `设计文档/data-retention.md` |
| 系统管理 | `/system/role`、`/system/user`、`/system/menu` | `设计文档/system-management.md` |
| 报告解读 | `/report/interpretation` | 已实现，9 页签状态机（20 生信看点 → … → 62 报告发送）；制品/状态/冻结口径见 §3.3 |
| 样本流转 | `/report/sample-flow` | 已实现，9 页签 + 处理（含生信认领）/ 重新驱动；口径见 §3.3 与 `docs/context/report/report-module-flow.md` |
| 样本信息 | `/report/sample-info` | 已实现（LIMS 侧录单样本，表 `sample_file`） |
| 报告模板 / 邮件配置 / 位点历史 | `/report/template`、`/report/mail-config`、`/report/variant-history` | 已实现 |
| 数据驱动（无页面） | `POST /report/drive/*` | 已实现，口径见 §3.2，外部由生信流程调用 |

角色：`wet_lab`（湿实验员）· `bioinformatician`（生信分析师）· `interpreter`（报告解读员）· `operation_admin`（运营管理员）。
权限点位形如 `read:QcControl` / `manage:ProductConfig` / `apply:CompanyAccess` / `approve:CompanyAccess`。

---

## 6. 构建、运行、验证（真实命令）

### 后端（在 `template-backend/` 下执行）

**本机没有安装 JDK / Maven**（`mvn: command not found`），所有构建/测试都走 Docker 里的 maven 镜像，
并复用宿主 `~/.m2` 缓存（离线 `-o` 可跑；首次没有缓存时去掉 `-o`）：

```bash
# 全量打包 → ruoyi-admin/target/ruoyi-admin.jar（后端容器以 bind mount 方式使用它）
docker run --rm --name biotech-plus-build \
  -v "$PWD":/src -v ~/.m2:/root/.m2 -w /src -e MAVEN_OPTS=-Xmx2g \
  maven:3.9-eclipse-temurin-17 mvn -B -ntp -o -Pdev -DskipTests package

# 只编译某个模块（连带依赖）：把 -pl 换成目标模块
#   ... mvn -B -ntp -o -pl ruoyi-modules/ruoyi-drive -am -DskipTests compile

# 跑单元测试：父 POM 默认 skipTests=true，且 Surefire 按 ${profiles.active} 过滤 @Tag，
# @Tag("local") 的测试只有 -Plocal 才会执行（-Pdev 会得到 "Tests run: 0"）
#   ... mvn -B -ntp -o -Plocal -pl ruoyi-admin -am -DskipTests=false \
#         -Dtest=ReportReinterpretServiceTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- 生效方式：`ruoyi-admin.jar` 是 bind mount 到容器 `/app/app.jar`，**重建 jar 后必须
  `docker restart biotech-plus-backend`**；启动日志搜 `Started DromaraApplication` 确认。
- 部署脚本不在本仓：`/home/liushangzhi/docker/biotech-plus/deploy.sh`（`build` / `build-image` / `restart-backend`）。
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
- **service 层包结构**（模块内协作类的落位规则，2026-10 定稿）：
  `service/` 及其子包**只放接口**；`service/impl/` 根放**入口服务**（被 Controller 注入、名字以 `Service`/`ServiceImpl` 结尾）
  与**能力组件**（`*Sender` / `*Variables` / `*Renderer` / `*Converter`，即使被 Controller 注入也留在根）；
  只被其他服务注入的**协作类**放 `service/impl/<域>/`，且**域内 ≥4 个类才建子包**（不足 4 个留在根，不建小包、不预留空包）。
  非 Bean 不放 service 层：**数据载体**（*Context / *Input）进 `domain/dto`；**跨层静态工具与常量**进 `utils/`
  （准入：无实例状态 + 被 ≥2 个不同层/域的类使用 + 不含业务判定口径；不满足则跟随它服务的域包）。
  完整条文见 `template-backend/docs/backend-code-standard.md`（含 `ruoyi-report` 的 `service/template` 模板扩展点包例外：
  该包策略接口与实现同包，按 `module_code` 维护 Handler 只进这一个包）。
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

- **`master` 落后于主干**：业务实现都在 `feat/audit-cdc`（比 master 多 27 个提交）。切到 `master`
  会看到"什么都没实现"的假象，别据此判断仓库状态。
- **驱动只覆盖 1238 的 10 个文件类型**：其余 30 类（tmb / her2 / mgmt / immune_all / neoantigen /
  mrd / pdinfo …）本轮未实现，对应老明细表在本地库也不存在；口径见 §3.2。
- **Python 驱动仓是独立 git 且带 WIP**：`docs/context/report/report-driver/` 里 `config/*.json` 大量未提交，
  只读参考；要改必须提交到它自己的仓，不要混进主仓提交。
- **没有 API 契约文件**：无 OpenAPI / 共享 TS 类型；接口以 Controller + 前端 `src/service/api` 为准。
- **文档存在死链**：`设计文档/README.md` 的流程图表链接指向 `docs/flowchart/...`，
  实际目录是 `docs/context/流程图/`。
- **部分设计文档与实现已偏离**：产品配置实际路由是 `/project/product-config`（不是 `/system/product-config`），
  质控标准页 `/project/qc-standard` 是新增的。**以代码为准**，文档只当需求输入。
- **`sample_qc_info` 字段未穷举**：文档写"另有 20+ 项指标"，缺完整清单。
- **交付文档里的截图是上一版行为拍的**：`docs/deliverables/10报告解读/截图/图18-提交审核核对.png` 等仍是
  「提交审核即冻结位点历史」的旧画面（文案已改、图未换）。重拍前先看
  `/home/liushangzhi/project/qa/biotech-plus/CAPTURE-RESHOT-TODO.md`（capture 脚本已改成数字状态码，
  但尚未在 QA 实跑验证）。
- **报告生成会在 docx 目录留下隐藏文件**：`.<报告名>.docx.renderer.log`（`ReportDocxRenderer` 的诊断副产物，
  随 docx 一起 move 进成品目录）。不想要就改它写到 `.work` 或应用日志。
- **「报告解读」列表默认按「分析日期=当天」过滤**：跨天回看历史批次会显示“无数据”，
  先展开搜索区改日期——这是既有默认值，不是接口没返回。
- **本仓库没有根 README**：本文件即入口；两个子项目 README 是 RuoYi / Soybean 模板自带的。

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
python3 tools/readability_check.py --changed-only origin/master   # 我这次改的合不合规（提 PR 前必跑；基线是 master，不是 main）
python3 tools/readability_check.py --rules                        # 规则清单
python3 tools/structure_check.py --changed-only origin/master     # 页面结构契约（缺 modules 组件 / 未用统一 hooks / 类型写错位置）
python3 tools/structure_check.py --rules                          # 结构规则清单
python3 tools/check_db_schema.py                                  # 数据库表规范体检（建表/改表后必跑，error 必须为 0）
python3 tools/check_db_schema.py --rules                          # 数据库规则清单
python3 tools/check_commit.py --latest 5                          # 校验最近 5 条提交信息（改完要提交时）
python3 tools/check_commit.py --rules                             # 提交规范清单
bash tools/install-git-hooks.sh                                   # 装 git 钩子（每个 clone 装一次即可）
python3 tools/new_crud.py --module <模块> --entity <实体> --table <表> --title <中文> --fields "..." --dry-run
git diff origin/master...HEAD > /tmp/change.diff                  # 交给评审者
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
