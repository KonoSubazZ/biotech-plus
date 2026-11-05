# Biotech-Plus · 药企 NGS 报告运营系统

## 项目是怎样运转的

项目和产品配置定义检测用到的标准与模板。样本登记后，实验室和生信人员分别查看质控结果、处理异常。生信文件由驱动模块解析入库，解读人员随后在报告模块完成解读、生成、审核、签名和发送。系统管理、审计和备份支撑这条业务线。[架构与目标流程图](docs/context/Project%20Workflow.png) · [现行报告设计 §0](docs/context/report/report-module-design.md)

## 第一次打开仓库，先看这几处

```text
docs/                         理解业务和运维
  modules/                    按功能查说明和验收资料，如 wet-lab-qc、sample-info
  context/report/             当前报告设计、流程和模板说明，与样本文件共用此目录
  context/设计文档/           查原始需求；使用前和当前实现核对
  guides/                     查看 CDC 运维和交付文档生成方法
template-frontend/            用户看到的管理系统
  src/views/                  按菜单找页面：project、sample、report、compliance、backup 等
  src/service/api/            找页面请求的地址、参数和返回类型
template-backend/             接口、业务规则和数据
  ruoyi-modules/              按职责找 Java 模块：项目、质控、驱动、报告、合规、审计、备份等
  ruoyi-admin/src/test/       查报告等功能的回归用例
  script/sql/business/        各业务模块的建表 SQL
  script/sql/update/          已有环境的版本迁移
tools/                        构建辅助、代码检查和生成脚本
ai-rules/、ai-templates/       开发约定和新模块模板
```

要改一个功能，先在[业务与代码对照表](docs/README.md)找那一行。例如湿实验质控的页面归“样本”，API 和后端归“质控”。报告模板的页面归“项目”，后端归“报告”。页面路径可对照[实际路由](template-frontend/src/router/elegant/routes.ts)。准备提交时按[开发规则](ai-rules/README.md)检查，自动化工具的入口在 [AGENTS.md](AGENTS.md)。

## 怎么知道改过什么

日常开发使用 `dev`，远程默认分支是 `master`。以下命令在仓库根目录执行：

```powershell
# 工作区有哪些待提交的改动
git status --short

# 当前分支最近有哪些已提交的改动
git log -8 --oneline

# 从默认分支分出后，本分支提交涉及哪些文件
git diff --stat origin/master...HEAD

# 只看报告审核相关的页面、后端和设计的历史
git log -5 --oneline -- template-frontend/src/views/report template-backend/ruoyi-modules/ruoyi-report docs/context/report
```

**一次实际变更：审核通过时用哪份数据冻结位点历史。** 提交 `d68b2f1` 保留审核通过时冻结的时机，将现场重算改为从本轮报告 JSON 读取位点内容。用 `git show --stat d68b2f1` 看它影响哪些文件，再对照[报告设计](docs/context/report/report-module-design.md)、[增量迁移](template-backend/script/sql/update/update_5.6.60-report-variant-edits.sql)和[冻结回归测试](template-backend/ruoyi-admin/src/test/java/org/dromara/test/report/HistoryFreezeFromArtifactTest.java)，可以分别回答“行为怎么变了、升级要做什么、什么测试守住这条规则”。

核对两个**上线版本**时，从发布记录取实际部署的两个标签或提交，用 `git diff --stat 旧提交 新提交` 比较；开发分支和生产环境可能处于不同提交。

## 本地启动

以下两组 PowerShell 命令分别从仓库根目录执行。后端需要 Java 17、Maven，以及开发环境可连接的 MySQL 和 Redis。先核对 [开发配置](template-backend/ruoyi-admin/src/main/resources/application-dev.yml) 的连接地址和凭据，默认业务库为 `biotech`。首次安装先准备空库，按发布版本确认初始化脚本。`init.sql` 含 `DROP TABLE`，仅用于全新空库。已有环境按已部署版本核对迁移记录，只执行尚未应用的增量迁移。

```powershell
Set-Location template-backend
mvn -B -ntp -Pdev "-DskipTests" package
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

后端默认监听 8080。使用 Docker 构建时见 [AGENTS.md §6](AGENTS.md)。在第二个终端启动前端，项目要求 Node ≥ 20.19.0、pnpm ≥ 10.5.0。

```powershell
Set-Location template-frontend
pnpm install
pnpm dev
```

打开 Vite 输出的地址。前端开发代理的目标和前缀见 [开发环境配置](template-frontend/.env.dev)。验证自己的改动时，后端按模块运行测试并核对实际测试数，前端运行 `pnpm typecheck`，改动提交前按[仓库检查清单](ai-rules/README.md)验证。
