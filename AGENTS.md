# AGENTS.md — Biotech-Plus 仓库工作约定

人员入门、启动步骤与发布依据见 [README](README.md)。日常开发使用 `dev`，默认分支为 `master`，提 PR 的检查基线统一使用 `origin/master`。

## 1. 开工前

- 先看[系统架构与业务流程](docs/context/Project%20Workflow.png)。修改 Java 先读[后端工作约定](template-backend/AGENTS.md)和[后端代码规范](template-backend/docs/backend-code-standard.md)。
- 修改前端先读[前端工作约定](template-frontend/.claude/CLAUDE.md)、[设计约束](template-frontend/DESIGN.md)和[交互契约](template-frontend/UX-CONTRACT.md)。若本地有 `.ai_state/`，先读 `.ai_state/_index.md`，避免扫描所有状态文件。
- 写代码或提 PR 前先读[协作规则](ai-rules/README.md)。新模块从 [ai-templates](ai-templates/) 生成。改动前说明文件与计划，保留已有工作区改动；只修改本次需求涉及的文件。

### 1.2 设计资料的适用范围

`docs/context/设计文档/` 和 `流程图/` 中有从其他项目逆向整理的需求。文中的 NestJS、React、PostgreSQL/Prisma 和权限装饰器属于来源项目。当前实现使用 Java/Spring Boot、Vue/Naive UI、MySQL/MyBatis-Plus 与 Sa-Token。设计与实现有出入时先查对应代码，技术栈、字段或权限仍不明确时询问维护者。

## 2. 文档地图

按问题查找[模块资料、报告规范、历史设计、运维与交付指南](docs/README.md)。旧设计中虚线“未实现”的标注描述设计时点；核对当前覆盖情况时，以本分支实现和测试为准。

## 3. 代码与业务边界

后端启动模块是 [ruoyi-admin](template-backend/ruoyi-admin/)，业务实现见 [ruoyi-modules](template-backend/ruoyi-modules/pom.xml)。前端从[页面](template-frontend/src/views/)、[API](template-frontend/src/service/api/)和[路由](template-frontend/src/router/elegant/routes.ts)定位。数据库使用后端 [SQL 目录](template-backend/script/sql/)，新迁移按[数据库规范](ai-rules/04-db-schema.md)编写。

### 3.2 驱动入库 `ruoyi-drive`（2026-10-21 落地，改这块前必读）

**这条链路已经全部在 Java 里了，不要再去找 / 调用 `report-driver/scanner.py`。**
旧 Python 驱动作为外部历史参考；本仓构建与运行使用 Java 实现。模块依赖只允许 `ruoyi-report → ruoyi-drive`，驱动模块不得反向依赖报告模块。

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
- **已入库 = 只读**（2026-11-05）：页面「来源」列 = 历史的行（history_* 已有该 match_key），临床意义、改靶、
  突变说明全部置灰/只读；对 `history_*` 的更新只剩命中历史时 `reuse_count+1` / `last_reused_at`。
  未入库位点的三类修改统一存 `analysis_report.report_variant_edits`（json，键 `gene_variant_sourceType:sourceId`），
  生成报告时写进 JSON；审核通过时**只读该 JSON** 冻结 `history_*`（不再现场重算），与制品不一致的历史值直接阻止审核（B1）。
- **突变说明**：默认由 HGVS 译文 + NKB 位点说明实时拼（不进冻结）；人工文本与父靶点/临床意义一起存
  `analysis_report.report_variant_edits`（键 `gene_variant_sourceType:sourceId`），空串 = 恢复默认；
  只有 31/32/33/52 能改（40+ 只读）。预览/生成共用 `assemble()` 应用，JSON 字段 `...items[].mutationDescription`。
  **历史 DOCX 模板不含该占位符**，要印用 `{{it.mutationDescription or ''}}`。

## 6. 构建、运行、验证

### 后端（在 `template-backend/` 下执行）

没有本地 JDK / Maven 的环境可使用 Docker。以下为 Bash 示例，复用宿主 Maven 缓存。首次构建缺少依赖时去掉 `-o`：

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

- 使用既有部署环境时：`ruoyi-admin.jar` 是 bind mount 到容器 `/app/app.jar`，**重建 jar 后必须
  `docker restart biotech-plus-backend`**；启动日志搜 `Started DromaraApplication` 确认。
- 部署脚本不在本仓：`/home/liushangzhi/docker/biotech-plus/deploy.sh`（`build` / `build-image` / `restart-backend`）。
- 改过检查规则后跑：`script/tests/checkstyle-regression.ps1`。
- 期望结果：末尾 `BUILD SUCCESS`。**核实实际执行的测试数量，零测试不等于通过。**

前端启动方式见[根 README](README.md)。在 `template-frontend/` 运行 `pnpm typecheck` 做类型检查，`pnpm gen-route` 生成路由。`pnpm lint` 会自动修复文件，运行后核对差异。

## 7. 工程约束与提交

- 按子目录契约实现分层、权限和界面交互。凭据从环境或配置读取。增加依赖须在交付说明中交代理由。
- 提交前按[检查清单](ai-rules/README.md)运行可读性与结构检查，改表另跑数据库检查。按[提交规范](ai-rules/05-git-commit.md)安装钩子并提交。一个分支同一时刻只允许一个写者，评审由另一位执行者负责。
- 前端 Athena 流程按[评审契约](ai-rules/02-review-contract.md)衔接。规则只写一处，能机械判定的规则同步维护检查器。

## 8. 交付格式

每次汇报按四段写，缺一不可：

1. **改了什么** —— 文件清单 + 每个文件做了什么
2. **验证了什么** —— 实际执行的命令
3. **真实终端输出** —— 原样粘贴，不许编造；未运行或受阻必须明说
4. **结论** —— 完成 / 未完成 / 遗留问题

写脚本或函数时另附：预期输出（含 2~3 组边界输入）、自测用例（`assert` + PASS/FAIL 打印）、真实运行结果。


## 9. 核对旧资料时

- 旧设计里的流程图链接曾指向 `docs/flowchart/`，对应文件在[需求侧流程图](docs/context/流程图/)；历史产品路由与[现行路由](template-frontend/src/router/elegant/routes.ts)有差异。接口以 Controller 和前端 API 为准。
- 旧资料对 `sample_qc_info` 的指标列举不全。报告的旧 QA 截图也可能仍显示“提交审核即冻结”；核验时用 §3.3 的冻结时机，并结合[交付流水线](docs/guides/交付文档流水线-使用文档.md)及外部 QA 工作目录的 `CAPTURE-RESHOT-TODO.md` 判断哪些图需重拍。
- 报告 DOCX 目录可能含 `.<报告名>.docx.renderer.log` 诊断文件。报告解读列表默认按分析当天过滤，历史批次需要切换日期查询。
