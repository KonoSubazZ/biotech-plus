# 按业务找文档与代码

先按业务问题找资料。核对 v1 实际行为时，以当前分支的实现、路由和版本迁移为依据；阅读旧设计时，区分需求来源、已实现行为和待验收项。

## 从要修改的业务开始

| 你要改什么 | 找页面或调用方 | 找接口和后端 | 查业务依据 |
|---|---|---|---|
| 项目、产品配置 | [项目管理页面](../template-frontend/src/views/project/) | [项目 API](../template-frontend/src/service/api/project/) → [ruoyi-project](../template-backend/ruoyi-modules/ruoyi-project/) | [产品配置说明](modules/product-config/) |
| 质控标准、湿实验与生信质控 | [质控标准](../template-frontend/src/views/project/qc-standard/) · [样本菜单](../template-frontend/src/views/sample/) | [质控 API](../template-frontend/src/service/api/qc/) → [ruoyi-qc](../template-backend/ruoyi-modules/ruoyi-qc/) | [标准](modules/qc-standard/) · [湿实验](modules/wet-lab-qc/) · [生信](modules/bioinfo-qc/) |
| 样本信息、样本流转 | [样本信息](../template-frontend/src/views/sample/sample-info/) · [样本流转](../template-frontend/src/views/report/sample-flow/) | [样本信息 API](../template-frontend/src/service/api/report/cms-sample.ts) · [流转 API](../template-frontend/src/service/api/report/sample-flow.ts) → [ruoyi-report](../template-backend/ruoyi-modules/ruoyi-report/) | [样本字段与说明](modules/sample-info/) |
| 生信文件解析入库 | 上游调用后端 [DriveController](../template-backend/ruoyi-modules/ruoyi-drive/src/main/java/org/dromara/drive/controller/DriveController.java) | [ruoyi-drive](../template-backend/ruoyi-modules/ruoyi-drive/) | [驱动入口和约束 §3.2](../AGENTS.md) |
| 解读、审核、签名、发送、模板 | [报告页面](../template-frontend/src/views/report/) · [项目下的模板配置](../template-frontend/src/views/project/template-config/) | [报告 API](../template-frontend/src/service/api/report/) → [ruoyi-report](../template-backend/ruoyi-modules/ruoyi-report/) | [现行报告设计 §0](context/report/report-module-design.md) |
| 合规记录、审计采集 | [合规页面](../template-frontend/src/views/compliance/)；CDC 在后台采集 | [合规 API](../template-frontend/src/service/api/compliance/) → [ruoyi-compliance](../template-backend/ruoyi-modules/ruoyi-compliance/)；[CDC 消费模块](../template-backend/ruoyi-modules/ruoyi-audit/) | [验证记录](modules/validation-records/) · [CDC 运维](guides/canal-cdc-运维.md) |
| 用户、角色、菜单与备份 | [系统管理](../template-frontend/src/views/system/) · [备份](../template-frontend/src/views/backup/) | [系统 API](../template-frontend/src/service/api/system/) → [ruoyi-system](../template-backend/ruoyi-modules/ruoyi-system/)；[备份 API](../template-frontend/src/service/api/backup/) → [ruoyi-backup](../template-backend/ruoyi-modules/ruoyi-backup/) | [用户说明](modules/user-management/) · [备份运维](modules/backup-db/) |

### 例：维护湿实验质控列表

- **改页面显示**：菜单入口 [wet-lab/index.vue](../template-frontend/src/views/sample/wet-lab/index.vue) 只挂载组件；列表内容从 [wet-lab-qc-page.vue](../template-frontend/src/views/sample/modules/wet-lab-qc-page.vue) 找。
- **改数据或判定**：[页面请求](../template-frontend/src/service/api/qc/wet-lab-qc.ts)调用 `GET /qc/wetLabQc/list`。沿 [WetLabQcController](../template-backend/ruoyi-modules/ruoyi-qc/src/main/java/org/dromara/qc/controller/WetLabQcController.java) 找到 [WetLabQcServiceImpl](../template-backend/ruoyi-modules/ruoyi-qc/src/main/java/org/dromara/qc/service/impl/WetLabQcServiceImpl.java)。只有涉及存储结构时才继续核对[建表 SQL](../template-backend/script/sql/business/wet-lab-qc.sql) 和[版本迁移](../template-backend/script/sql/update/)。
- **核对影响**：用[湿实验质控开发测试文档](modules/wet-lab-qc/湿实验质控开发测试文档.md)核对受影响的场景；行为变化时在同一次提交更新说明与相关测试。

## 查详细资料

| 想查什么 | 入口 |
|---|---|
| 系统边界与样本到报告的目标流程 | [架构与流程图](context/Project%20Workflow.png) |
| 报告状态、位点冻结与制品 | [报告设计 §0](context/report/report-module-design.md) · [流程图（含旧流程）](context/report/report-module-flow.md) · [渲染](context/report/report-docx-renderer.md) |
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
