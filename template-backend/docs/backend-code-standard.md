# 后端代码规范

## 范围与执行方式

本规范适用于本后端仓库。人和 AI 遵循同一套约定：本文说明工程规则，根目录 `AGENTS.md` 提供 AI 执行入口，Checkstyle 执行可自动验证的规则，`.editorconfig` 统一基础格式。

当前 Maven 聚合模块为 `ruoyi-admin`、`ruoyi-common`、`ruoyi-extend`、`ruoyi-modules`。检查随父 POM 继承，覆盖各模块 `src/main/java` 与 `src/test/java`；资源文件、编译输出和构建生成源码不在首批检查范围。未接入 Maven 聚合构建的目录不会自动被扫描。

## 分层与命名

- Controller：处理 HTTP 参数、校验、权限、调用业务服务和封装返回值；新增复杂业务逻辑放在 Service。
- Service：业务校验、事务边界、跨 Mapper 操作；多步骤写入需要原子性时使用事务，并明确异常回滚行为。
- Mapper：数据库访问；复用现有 MyBatis-Plus 基类与查询方式，避免循环查询造成 N+1 问题。
- Entity 放在 `domain`；请求业务对象放在 `domain/bo`；响应对象放在 `domain/vo`。敏感字段不能因直接返回实体而泄漏。
- 包名使用小写分段，类名使用 UpperCamelCase，方法与变量使用 lowerCamelCase，常量使用 UPPER_SNAKE_CASE。
- 沿用 `XxxController`、`IXxxService`、`XxxServiceImpl`、`XxxMapper`、`XxxBo`、`XxxVo` 等模块既有命名；不要为套用命名规则重命名无关历史代码。
- 优先构造器注入；已有 Lombok 场景使用 `@RequiredArgsConstructor` 与 `private final` 依赖。

## 可读性优先

允许适度啰嗦，以读者能顺着代码理解业务为目标，不以行数少作为优化标准。

- 使用明确的局部变量保存权限判断、查询条件和中间结果；一次操作中的同一权限判断只计算一次，避免重复调用上下文方法。
- 权限、租户和可选条件使用独立的 `if` 块，分支需要的值在分支内读取，不把分支判断与三元表达式塞进同一个方法调用。
- 查询按“声明 Wrapper → 基础条件 → 条件分支 → 排序 → 执行”展开；简单且含义一致的设置可以使用短链式调用。
- 多层方法调用或过长表达式拆成有业务含义的步骤；分支使用大括号，不把判断、操作和返回值压缩在同一行。
- 局部变量说明它代表什么；注释说明业务原因，不重复翻译代码。不为了减少几行代码引入难以理解的抽象。
- 新增和修改的方法按此风格编写；未涉及的历史代码不做全量风格重排。这些可读性要求由代码评审检查，不以禁止所有链式调用或三元表达式来替代判断。

以租户选项查询为例：

```java
public List<SysTenant> options() {
    boolean isSuperAdmin = LoginHelper.isSuperAdmin();
    LambdaQueryWrapper<SysTenant> wrapper = new LambdaQueryWrapper<>();
    wrapper.select(SysTenant::getId, SysTenant::getTenantId, SysTenant::getTenantName, SysTenant::getStatus)
        .eq(SysTenant::getStatus, "0");

    if (!isSuperAdmin) {
        wrapper.eq(SysTenant::getTenantId, TenantContext.getTenantId());
    }

    wrapper.orderByAsc(SysTenant::getId);
    return mapper.selectList(wrapper);
}
```

这里明确区分了基础过滤和权限过滤，超级管理员路径不读取租户上下文，也不会重复计算管理员身份。

## 接口、安全与数据

- 普通 JSON 接口使用 `R<T>`；分页使用 `PageQuery` 与 `TableDataInfo<T>`；文件导出、流式响应按现有接口模式实现。
- 使用 Jakarta Validation 与 `@Validated` 校验请求，必填、长度、范围、集合为空等约束与业务一致。
- 管理接口检查 `@SaCheckPermission` 或项目等效授权机制；明确公开接口的理由。业务层仍需验证数据归属，不能只检查菜单权限。
- 写操作按业务需要使用 `@Log`、`@RepeatSubmit`；防重复提交不能替代数据库唯一约束和业务幂等。
- 租户归属使用服务端可信上下文及现有租户组件；批量更新、删除、导出需验证所有对象的访问权限。
- 使用参数化查询，禁止直接拼接用户输入。排序与动态标识符使用白名单；必要的动态 SQL 需说明安全边界。
- 业务异常沿用项目异常体系，异常不能静默吞掉；日志使用现有 SLF4J 方式，并保留必要异常堆栈。
- 密钥、密码、Token 从环境或外部配置读取；示例使用占位符。日志按需脱敏，不输出完整凭据或敏感请求。
- 数据库结构变更在 `script/sql/update` 提供迁移脚本并说明兼容、数据迁移及回滚方案；不要只修改实体。

## SQL 编写与验证

| 场景 | 推荐实现 |
| --- | --- |
| 单表增删改查、简单条件组合、排序、计数 | 复用 `BaseMapperPlus` 与 `LambdaQueryWrapper` / `LambdaUpdateWrapper` |
| 单表分页 | 使用项目现有 `PageQuery`、MyBatis-Plus 分页能力与 `TableDataInfo` |
| 简单单表聚合 | MyBatis-Plus 能清晰表达时可以使用；表达复杂时转为 XML |
| 多表关联、嵌套子查询、UNION、复杂聚合报表、复杂动态 SQL | 在 Mapper XML 编写完整 SQL，通过 Mapper 接口调用 |

按查询的实际复杂度选择实现，不以 SQL 长度或是否出现某个关键词机械划分。不要为了坚持使用 Wrapper，把复杂 SQL 整段塞进 `apply`、`inSql`、`exists`、`last`，或在 Controller/Service 中拼装 SQL。涉及固定列的简单查询优先使用 Lambda 字段引用。

Mapper XML 放在所属模块的 `src/main/resources/mapper/` 下，文件以 `Mapper.xml` 结尾，与项目的 `classpath*:mapper/**/*Mapper.xml` 扫描配置一致。`namespace` 对应 Mapper 接口全限定名，语句 `id` 对应接口方法名；多个参数使用明确的 `@Param` 名称。复杂查询按需定义 `resultMap`，避免 `SELECT *` 引起联表字段冲突或泄漏敏感字段。

普通值使用 `#{param}` 绑定；Wrapper 使用参数化条件，不接收前端传入的 SQL 片段。`${}` 是文本替换，不能用于普通值参数。项目已有 `${ew.getCustomSqlSegment}` 接入点仅能接收服务端构建的可信 Wrapper，其存在不代表任意 `${}` 拼接安全。确需动态表名、列名或排序字段时，必须经过服务端白名单校验并单独评审。

**构建自动检查**：禁止 MyBatis 的 `@Select`、`@Insert`、`@Update`、`@Delete`、对应的 `*Provider` 以及 `@SelectKey`，包括重复注解容器 `.List`。需要手写 SQL 时放入 XML；`@Mapper`、`@Param` 等非 SQL 注解仍可使用。检查基于 Java 语法树及导入信息，覆盖普通导入、通配符导入和全限定写法，不因注释、字符串或其他包的同名注解误报。

**评审与测试检查**：确认复杂 SQL 已落在 XML；验证 `namespace`、语句 `id`、参数名、结果映射、空集合条件、分页记录及总数；涉及租户数据时覆盖同租户与跨租户场景，不能假设所有自定义 SQL 都会被拦截器正确改写。复杂查询需要在对应数据库上验证实际结果；性能敏感查询结合执行计划检查索引和扫描范围。

静态检查不连接数据库、不判断 SQL 业务正确性，也不自动判定查询复杂度或解析 Mapper XML。历史 Java SQL 本次未批量迁移，后续涉及相应查询时按本节规范调整并补回归测试。迁移和数据库管理脚本按其独立用途评审。

## 格式与自动检查

沿用 `.editorconfig`：Java 四空格缩进，UTF-8，LF，去除行尾空白并保留文件末尾换行；JSON/YAML 两空格。IDE 格式化不能覆盖这些约定。

首批 Checkstyle 强制规则：

| 规则 | 目的 |
| --- | --- |
| FileTabCharacter | Java 文件禁止 Tab 缩进 |
| PackageName、TypeName | 约束包名及类型名称 |
| OuterTypeFilename | 顶层类型与文件名一致 |
| UpperEll | long 字面量使用大写 L，避免与数字 1 混淆 |
| EmptyStatement | 拒绝多余的空语句 |
| OneStatementPerLine | 每行最多一条语句 |
| IllegalImport | 禁止导入 sun、com.sun 内部 JDK 包 |
| MatchXpath（SQL 注解） | 禁止通过 MyBatis SQL 注解或 Provider 在 Java 中编写 SQL |

本阶段保留历史 import 顺序、通配符导入与换行习惯。方法命名、事务、权限、SQL 安全等未被本配置完全检查的规则，通过代码评审和测试验证。后续增加自动规则时先评估现有代码，不用全局 suppress 或关闭失败机制掩盖问题。

## 本地与 CI 验证

以下命令从本后端仓库根目录执行。先确认 Maven 使用 JDK 17；如需切换，配置本机实际的 `JAVA_HOME`，不把个人安装路径写入仓库。

```powershell
mvn -version
mvn -B -ntp validate
```

预期：各聚合模块成功，末尾出现 `BUILD SUCCESS`。Checkstyle 绑定 `validate`，因此正常的 `compile`、`test`、`package`、`verify` 也会先执行检查；`validate` 自身不编译代码，也不启动数据库或 Redis。

CI 应从后端根目录执行相同检查命令，并将失败作为合并阻断条件；本次仅接入 Maven，不配置具体托管平台的流水线或分支保护。

定向模块检查仍从根目录执行：

```powershell
mvn -B -ntp -pl ruoyi-modules/ruoyi-system -am validate
```

测试需显式关闭默认跳过，并注意父 POM 按 `${profiles.active}` 筛选 JUnit 标签。公共模块不一定含 JUnit 引擎，不能直接对所有模块启用标签测试。先构建受影响的依赖，再在含测试依赖的模块执行；下面以现有租户测试为例，选择 `dev` 标签。执行后核实实际测试数量，不能把零测试当作通过。

```powershell
mvn -B -ntp -pl ruoyi-modules/ruoyi-system -am '-DskipTests=true' install
if ($LASTEXITCODE -ne 0) { throw '依赖构建失败' }
mvn -B -ntp -Pdev -pl ruoyi-admin '-DskipTests=false' '-Dtest=TenantIsolationTest,TenantOwnershipTest' test
```

修改检查配置后的独立回归验证：

```powershell
powershell -NoProfile -File .\script\tests\checkstyle-regression.ps1
```

预期：合规源码及字符串/注释边界通过；main/test 中的违规样例使 Maven 失败，并匹配预期规则；MyBatis-Plus 与无 SQL 注解的 Mapper 接口通过，MyBatis SQL 注解的不同写法被拒绝，其他包的同名注解通过。样例和原始 Maven 日志保留在 `target/checkstyle-regression` 下；自测不修改业务源码。

## IDE 与 AI

- IntelliJ IDEA：启用 EditorConfig，项目 SDK 与 Maven Runner 使用 JDK 17。可选安装 CheckStyle-IDEA，加载本仓库 `config/checkstyle/checkstyle.xml`，引擎版本与 POM 一致；插件版本不支持时，以 Maven 结果为准。
- VS Code：启用 EditorConfig 和 Java 开发支持，通过 Maven 执行统一检查；不另建一套与仓库冲突的规则。
- AI：后端入口为根目录 `AGENTS.md`。从更上层工作区发起任务时，明确要求读取后端的 `AGENTS.md`。指令约定不能替代 Maven 检查和真实测试。

参考：[Maven Checkstyle check 参数](https://maven.apache.org/plugins/maven-checkstyle-plugin/check-mojo.html)、[Checkstyle 引擎版本配置](https://maven.apache.org/plugins/maven-checkstyle-plugin/examples/upgrading-checkstyle.html)、[Checkstyle MatchXpath](https://checkstyle.org/checks/coding/matchxpath.html)、[MyBatis Mapper XML](https://mybatis.org/mybatis-3/sqlmap-xml.html)。
