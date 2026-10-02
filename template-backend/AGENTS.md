# 后端协作规范

作用范围：本目录及子目录。修改前阅读 `docs/backend-code-standard.md`，与现有代码保持一致。

> 仓库级规则（可读性判据 / 评审契约 / 多人协作）见 `../ai-rules/`，
> 机械闸门：在仓库根目录执行 `python3 tools/readability_check.py --changed-only origin/main`。
> 新模块从 `../ai-templates/backend-module/` 复制，不要从零写。

## 开发约定

- 使用 Java 17、Maven 和现有 Spring Boot / MyBatis-Plus 技术栈；先查找已有实现再增加依赖或抽象。
- 改动前说明文件清单和计划；保留用户未提交的改动，不顺手批量格式化或重构。
- Controller 负责路由、参数校验、权限与响应；Service 负责业务规则和事务；Mapper 负责持久化。
- 沿用 `domain`、`domain/bo`、`domain/vo` 分工，普通接口返回 `R<T>`，分页返回 `TableDataInfo<T>`；导出、流式接口按已有模式处理。
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

## 验证与交付

- 终端使用 PowerShell 兼容命令；执行前说明目的，先确认 `mvn -version` 使用 JDK 17。
- 从本后端仓库根目录执行 `mvn -B -ntp validate`；定向模块使用根目录的 `-pl` 参数。
- 功能逻辑变更增加断言测试，覆盖正常路径及空值、越权、重复请求等相关边界。
- 测试默认被父 POM 跳过；运行测试须显式传 `-DskipTests=false`，并匹配 Surefire 的环境标签筛选，核实实际执行数量。
- 修改检查规则后运行 `script/tests/checkstyle-regression.ps1`，验证通过和拒绝两类样例。
- 交付按“改了什么、验证了什么、真实终端输出、最终结论”汇报；未运行或受阻的验证必须明确说明。
