# Biotech-Plus · 药企 NGS 报告运营系统

本仓库维护从样本登记、实验和生信质控，到报告解读、审核、签名与发送的业务系统。Java 后端和 Vue 前端分别构建。日常开发使用 `dev`，默认分支是 `master`，仓库检查统一以 `origin/master` 为基线。

| 要做的事 | 从这里开始 |
|---|---|
| 找业务说明、接口、验收材料或运维指南 | [文档导航](docs/README.md) |
| 修改后端 | [业务模块](template-backend/ruoyi-modules/) · [后端约定](template-backend/AGENTS.md) |
| 修改前端 | [页面](template-frontend/src/views/) · [API](template-frontend/src/service/api/) · [实际路由](template-frontend/src/router/elegant/routes.ts) |
| 开发、评审和提交 | [协作规则和检查命令](ai-rules/README.md) · [AI 工作入口](AGENTS.md) |
| 核对上线版本和迁移 | 先确定发布提交或标签，再核对该版本的 [增量迁移](template-backend/script/sql/update/) 和 [新空库初始化 SQL](template-backend/script/sql/init.sql) |

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
