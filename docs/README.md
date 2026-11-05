# 维护文档导航

先按业务问题找资料。核对 v1 实际行为时，以当前分支的实现、路由和版本迁移为依据；阅读旧设计时，区分需求来源、已实现行为和待验收项。

| 想查什么 | 入口 |
|---|---|
| 系统边界与样本到报告的目标流程 | [架构与流程图](context/Project%20Workflow.png) |
| 报告状态、位点冻结与制品 | [报告设计 §0](context/report/report-module-design.md) · [业务流程](context/report/report-module-flow.md) · [渲染](context/report/report-docx-renderer.md) |
| 报告匹配、签名与模板 | [匹配规则](context/report/report-match-rules.md) · [电子签名](context/report/report-sign-ssq.md) · [模板说明](context/report/report-template-pharma-shengyu.md) |
| 样本、质控与项目 | [样本信息及 CMS 字段](modules/sample-info/) · [湿实验质控](modules/wet-lab-qc/) · [生信质控](modules/bioinfo-qc/) · [产品配置](modules/product-config/) · [质控标准](modules/qc-standard/) |
| 管理与合规 | [用户](modules/user-management/) · [角色](modules/role-management/) · [菜单](modules/menu-management/) · [租户](modules/tenant-management/) · [样本审计](modules/sample-audit/) · [开发记录](modules/dev-log/) · [验证记录](modules/validation-records/) |
| 备份、CDC 与客户交付 | [备份操作与运维](modules/backup-db/) · [Canal CDC 运维](guides/canal-cdc-运维.md) · [交付文档流水线](guides/交付文档流水线-使用文档.md) |
| 发布前核对数据库 | [仅用于新空库的初始化脚本](../template-backend/script/sql/init.sql) · [业务 DDL](../template-backend/script/sql/business/) · [增量迁移](../template-backend/script/sql/update/) · [数据库规范](../ai-rules/04-db-schema.md) |
| 追溯原始需求与旧结构 | [原工程设计](context/设计文档/) · [需求侧流程图](context/流程图/) · [2026-10 报告表结构快照](context/report_management_schema.sql) |

旧设计有按其他技术栈编写的路由和数据模型，部分文件又补录了本仓的字段变更。例如产品配置要结合 [模块说明](modules/product-config/) 和 [实际路由](../template-frontend/src/router/elegant/routes.ts) 核对。报告制品和状态则先读报告设计的现行快照，并结合 [AGENTS.md §3.3](../AGENTS.md) 查看补充约定。

## 新文档放在哪里

- 单模块的功能、接口、数据约定、测试与使用说明放 `docs/modules/<现有模块名>/`，随对应代码变更更新。报告域在后续迁移前仍维护上表所链接的现行资料。
- 涉及多个模块的操作和运维说明放 `docs/guides/`。跨端开发规范维护在 [ai-rules](../ai-rules/README.md)，后端代码规范和前端设计约定分别留在工程内。
- 模块目录中已经跟踪的 Markdown 与 Word 配对保留。修改前确认源稿与生成方式，客户交付件按交付流水线生成到 `docs/deliverables/`，该目录由 [Git 忽略规则](.gitignore) 管理。
- 新增或替代现行规范时，注明适用版本、维护模块与核验依据。旧资料注明其替代入口；运行日志、临时状态和未经确认的样本不作为规范提交。
