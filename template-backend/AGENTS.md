# 后端协作规范

作用范围：本目录及子目录。修改前阅读 `docs/backend-code-standard.md`，与现有代码保持一致。

> 仓库级规则（可读性判据 / 评审契约 / 多人协作）见 `../ai-rules/`，
> 机械闸门：在仓库根目录执行 `python3 tools/readability_check.py --changed-only origin/master`
> （本仓默认分支是 `master`，**没有 `main`**）。
> 新模块从 `../ai-templates/backend-module/` 复制，不要从零写；注册新模块要同时改 3 处 POM：
> `template-backend/pom.xml`（dependencyManagement）、`ruoyi-modules/pom.xml`（`<modules>`）、
> `ruoyi-admin/pom.xml`（依赖），漏一处会在 `mvn` 解析 POM 阶段直接失败。

## 开发约定

- 使用 Java 17、Maven 和现有 Spring Boot / MyBatis-Plus 技术栈；先查找已有实现再增加依赖或抽象。
- 改动前说明文件清单和计划；保留用户未提交的改动，不顺手批量格式化或重构。
- Controller 负责路由、参数校验、权限与响应；Service 负责业务规则和事务；Mapper 负责持久化。
- 沿用 `domain`、`domain/bo`、`domain/vo` 分工，普通接口返回 `R<T>`，分页返回 `TableDataInfo<T>`；导出、流式接口按已有模式处理。
- **service 层包结构**（协作类落位，2026-10 定稿，完整条文见 `docs/backend-code-standard.md`）：`service/` 及子包只放接口；
  `service/impl/` 根放入口服务（被 Controller 注入的 `XxxService`/`XxxServiceImpl`）与能力组件（`*Sender`/`*Variables`/`*Renderer`/`*Converter`）；
  只被其他服务注入的协作类进 `service/impl/<域>/`，**域内 ≥4 个类才建子包**；数据载体进 `domain/dto`；
  跨层静态工具与常量进 `utils/`（准入：无实例状态 + 被 ≥2 个不同层/域使用 + 不含业务判定口径）。
  例外：`ruoyi-report/service/template/` 是模板扩展点包（接口与实现同包，按 `module_code` 维护 Handler 只进这一个包）。
- 优先构造器注入和 `@RequiredArgsConstructor`；复用现有异常、分页、日志、映射和权限组件。
- 可读性优先于代码行数：允许多写局部变量和步骤，使用清晰的业务命名；权限、租户及可选查询条件用独立 `if` 表达，避免嵌套三元表达式、重复上下文调用和过长链式调用。
- 构建查询时先声明 Wrapper，再设置基础条件、分支条件和排序，最后调用 Mapper；条件分支所需的上下文值在分支内读取。简单且含义一致的条件可以保持短链式调用，不为减少行数压缩业务逻辑。
- 新增或修改管理接口时检查权限、参数校验和数据权限；写接口按业务需要配置操作日志和防重复提交。
- 租户标识与归属以服务端可信上下文为准；逐项检查批量操作权限，禁止直接信任请求中的租户、用户或部门标识。
- 单表 CRUD、简单条件查询和分页优先使用 MyBatis-Plus 的 `BaseMapperPlus`、Lambda Wrapper 及现有分页插件；复杂联表、子查询、聚合报表与复杂动态 SQL 写在 `src/main/resources/mapper/**/*Mapper.xml`，通过 Mapper 接口调用。
- 禁止使用 MyBatis CRUD SQL 注解、SQL Provider 注解或 `SelectKey` 编写 SQL；不在 Controller/Service 拼装复杂 SQL，也不把整段复杂 SQL 塞进 Wrapper 的 `apply`、`inSql`、`last`。
- SQL 值参数使用 `#{}` 或 Wrapper 参数绑定；动态列名、排序字段须白名单校验。复杂 XML 查询需验证租户/数据权限、分页总数及空集合边界。涉及数据库变更时提供可追踪迁移脚本。
- 不硬编码密钥、密码、Token；从环境或外部配置读取。不在日志输出密码、Token 或完整敏感请求。
- 按 `.editorconfig` 使用 UTF-8、LF、四空格缩进；自动检查以 `config/checkstyle/checkstyle.xml` 为准，不为通过检查直接关闭规则。
- **报告域（`ruoyi-report`）状态与制品口径**：`analysis_report.report_status` 是数字码，唯一权威是
  `utils/ReportStatus.java`（**不要写英文枚举常量**）；制品目录、生信认领、62 只读、「历史只在审核通过时冻结」
  这四条见根目录 `AGENTS.md` §3.3，细节见 `docs/context/report/report-module-design.md` §0.7。
  `report.artifact.root` 按环境放在 profile（dev=`/app/reports`），不要写进共用 `application.yml`。

## 验证与交付

- **本机没有 JDK / Maven**（`mvn: command not found`），一律用 Docker 构建，命令见根目录 `AGENTS.md` §6：
  `docker run --rm -v "$PWD":/src -v ~/.m2:/root/.m2 -w /src maven:3.9-eclipse-temurin-17 mvn -B -ntp -o -Pdev -DskipTests package`。
- 只编译单模块：加 `-pl ruoyi-modules/<模块> -am`；全量打包产出 `ruoyi-admin/target/ruoyi-admin.jar`，
  它是 bind mount 到后端容器 `/app/app.jar` 的，**重建后需 `docker restart biotech-plus-backend`** 才生效。
- 功能逻辑变更增加断言测试，覆盖正常路径及空值、越权、重复请求等相关边界。
- 跑测试：父 POM 默认 `skipTests=true`，必须 `-DskipTests=false`；Surefire 按 `${profiles.active}` 过滤 `@Tag`，
  写 `@Tag("local")` 的测试**只有 `-Plocal` 才会执行**（用 `-Pdev` 会得到 `Tests run: 0`，别当通过）。
  建议：`-Plocal -Dtest=<类名> -Dsurefire.failIfNoSpecifiedTests=false`。
- 修改检查规则后运行 `script/tests/checkstyle-regression.ps1`，验证通过和拒绝两类样例。
- 交付按“改了什么、验证了什么、真实终端输出、最终结论”汇报；未运行或受阻的验证必须明确说明。
