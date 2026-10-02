# 轻量租户版本

本分支使用同库同表隔离，租户标识为 `tenant_id`。创建用户必须指定有效租户；用户所属租户创建后不可修改。同一租户的用户共享该租户的数据，不同租户相互隔离。登录用户名全局唯一，无需在登录页选择租户。

现有 `user_id=1` 的 admin 是平台管理员，可查看全部租户数据，并在系统管理第一个菜单管理租户。超级管理员依据用户 ID 判断，修改用户名或分配普通角色不会产生平台权限。

## 数据库迁移

在当前 RBAC-only 数据库执行 `script/sql/update/update_5.6.2-tenant-version.sql`。脚本可重复执行，已有用户和业务数据归入默认租户 `000000`。菜单、角色、字典、配置、客户端、生成器配置和任务调度基础表保持全局共享；普通租户账号不能修改全局配置。

默认租户不能删除或停用。有用户或业务数据的租户不能删除，可以停用；停用后新登录和已有会话请求都会被拒绝。公开注册关闭，账号由管理员创建。

不要重新执行旧的 RBAC-only 去租户脚本。原始全量初始化脚本不是本次增量迁移的替代品。

## 后续业务表接入

1. 新业务表添加 `tenant_id varchar(20) NOT NULL` 及以租户字段开头的查询索引，实体继承 `TenantEntity`。
2. 使用 MyBatis-Plus / MyBatis Mapper 查询、更新和删除，由租户拦截器过滤。普通用户创建时服务端自动填充当前租户，忽略客户端伪造值；更新不会改变归属。
3. 管理员创建跨租户业务记录时，应由管理接口显式选择并校验有效租户；未指定时使用管理员所属租户。
4. 原生 JDBC、自定义显式插入 `tenant_id`、异步任务、第三方匿名回调不能依赖 HTTP 登录上下文。必须显式绑定可信租户（`TenantContext.withTenant`），并校验对象归属。禁止使用请求提供的租户编号切换查询上下文。
5. 不要把业务表加入共享表白名单，也不要在业务接口使用 `withoutTenant`。跨租户缓存必须在 key 中包含租户标识；现有用户昵称、文件信息缓存已按租户划分。

## 验证

预期：两个租户只能访问各自用户和公告；admin 可以查看两边。边界：伪造租户字段无法迁移归属；混合租户批量操作在写入前拒绝；停用租户的已有会话失效。

使用 JDK 17，先构建依赖，再运行指定测试：

```powershell
mvn.cmd -pl ruoyi-admin -am install '-DskipTests=true' -q
mvn.cmd -pl ruoyi-admin test '-Dtest=TenantIsolationTest,TenantOwnershipTest' '-DskipTests=false' -q
python script/tests/tenant_regression.py
```

真实 API 回归需要本机 Docker 容器 `mysql-5.7`（3306，源库 `ry-vue`）和 `ruoyi-redis-5x`（6380），Python 安装 requests、bcrypt。凭据从容器环境读取，不写入代码。脚本使用随机命名的临时数据库、Redis 15、18081 端口，连续迁移两次后运行实际 HTTP 断言，结束时关闭测试服务器并删除该临时库。测试账号密码仅在内存生成；正式数据库密码不变。运行中的 JAR 必须先关闭服务器再重新打包。

## 本次验收（2026-10-02）

后端 7 项单元测试通过，实际 API 回归 47 项断言通过，包含保留平台日志编号拒绝创建。浏览器验证 admin 的租户菜单首项、新增成功、必填校验、编辑编号锁定，以及普通用户仅能看本租户、连续两次打开新增用户仍保留所属租户。独立复审无剩余阻塞问题。

前端 ESLint、Oxlint、Vue 类型检查、生产构建及 DESIGN.md 格式检查通过。本次租户/用户/自定义组件范围 UI 审查无问题；全项目审查有两处原有主题文本框/滚动条静态发现，记录在前端 `.ai_state/details/premium-audit.json`。原有 pnpm 工作区依赖策略配置问题未改，验证和启动采用已安装的 `.\node_modules\.bin\*.cmd`。

本地开发库完成增量迁移，用户 6 条、公告 2 条、用户角色 5 条均保留。前端 9527、后端 8081 和代理均返回 HTTP 200。临时测试库已清理，正式 admin 密码不变。尚未提交或推送。

实际终端输出摘录：

```text
TENANT_REGRESSION: PASS (47 assertions)
Build successful. Please see dist directory
FRONTEND: HTTP 200
BACKEND: HTTP 200
PROXY: HTTP 200
QA_DATABASES_REMAINING: 0
```

租户拦截器依据 [MyBatis-Plus 官方文档](https://baomidou.com/plugins/tenant/) 接入；自动填充和实体更新策略额外控制写入归属，不能依赖拦截器纠正自定义 SQL 中显式提供的租户值。
