# 报告流程更新验证记录

日期：2026-10-18。开发分支 `codex/report-regeneration-flow`，起点 `1c66288`。

## 提交与实现范围

| 提交 | 范围 |
|---|---|
| `648da03` | 数据库迁移、业务版本登记、生成制品发布与恢复、原审核人复审、签名回调防串轮次、发送互斥、重新解读接口及后端测试 |
| `3d5d8f7` | 状态按钮、重新解读入口、版本展示、可编辑生成参数、复审权限、前端设计契约 |
| 本记录所在提交 | 独立 MySQL 回归脚本、验收用例与真实验证证据 |

数据库入口为 `template-backend/script/sql/update/update_5.6.22-report-regeneration.sql`；后端实现主要位于 `ruoyi-report` 的 `service/impl/revision/`、生成／审核／签名／发送服务、重新解读控制器以及对应 Mapper。测试位于 `ruoyi-admin/src/test/java/org/dromara/test/report/`。

版本在生成时占号，成功发送后消耗；未发送的重复生成、审核驳回、签名或发送失败不递增。已发送记录不能覆盖，重新解读创建关联的新报告。同一租户、客户、样本和模板的序列跨批次连续，且只允许一份在制报告占号。

名称主体在首次占号生成时固化，截短时预留最大整数版本后缀的位置；同序列后续版本、同版本重试都保持主体一致。制品按租户／报告／版本隔离，生成轮次用于失败和驳回归档。

`docs/architecture-review.md` 为原有未跟踪文件，未纳入本次提交。没有执行合并或推送；没有迁移实际业务库、回填历史版本或清理旧制品。

## 后端定向测试

环境：Windows PowerShell、JDK 17、Maven 3.9.9。父 POM 默认跳过测试，因此显式使用 `-DskipTests=false` 和 `local` profile；实际执行 47 项，跳过 0 项。

在 `template-backend` 执行：

```powershell
mvn -B -ntp -pl ruoyi-modules/ruoyi-report -am '-DskipTests=true' install
mvn -B -ntp -Plocal -pl ruoyi-admin '-DskipTests=false' '-Dtest=ReportRevisionPolicyTest,ReportArtifactStoreTest,ReportVersionAllocatorTest,ReportOperationLockTest,ReportReinterpretServiceTest,InterpretationRevisionReviewTest,ReportRevisionTenantSqlTest,ReportSignGenerationCallbackTest,ClusterDriverInvokerTest,SampleFlowReviewClaimTest,ReportNameResolverTest,ReportMailSendServiceTest' test
```

真实输出节选：

```text
[INFO] Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  13.015 s
[INFO] Finished at: 2026-10-18T10:20:21+08:00
```

覆盖允许／禁止生成的状态、原审核人与越权限制、源模板继承、制品发布失败恢复及归档、长文件名和 V9/V10/最大版本后缀、发送成功后登记异常、旧合同回调、互斥锁及事务结束释放。驱动测试实际启动 Python 子进程，验证参数、单行 JSON、大输出、退出失败和超时；邮件、上上签等外部服务使用测试替身。

完整本地 Maven 输出保留于 `template-backend/target-report-regeneration-tests.log`，该文件不提交。

## 真实 MySQL 回归

使用已有本地镜像 `mysql:8.0.42` 创建无网络、无公开端口、tmpfs 数据目录的临时容器 `codex-report-regeneration-mysql`，仅测试专用库 `codex_report_regeneration_test`。脚本由仓库真实 DDL 建库，执行新迁移两遍，并调用实际 Mapper XML 中的 SQL；并发断言使用两个数据库连接。

在仓库根目录执行：

```powershell
$env:PYTHONIOENCODING='utf-8'
python template-backend/script/tests/test_report_regeneration_db.py --keep-db
```

预期：首次发送 V1 后下一次读取 V2；失败与重复生成不消耗版本；不同租户互不影响；停用和跨租户审核人不通过；两个连接争抢同序列时能看到已提交的占号人。

真实完整输出：

```text
PASS: MySQL 8 runtime
PASS: new migration is repeatable over actual report DDL
PASS: first base name is immutable across regenerations and later versions
PASS: sign failure regeneration clears old signature and keeps reviewer
PASS: regeneration increments attempt only, preserves V1
PASS: unsent and failed attempts do not consume a version
PASS: successful send consumes V1 and releases owner
PASS: sent report is completed
PASS: next allocation reads last sent version for V2
PASS: same sample and template are isolated by tenant
PASS: unique sequence key covers tenant client sample template
PASS: normal reviewer status 0 accepted
PASS: platform reviewer accepted
PASS: other tenant reviewer rejected
PASS: disabled reviewer rejected
PASS: resubmit goes directly to original reviewer queue
PASS: failed generation cannot submit
PASS: concurrent allocator observes committed owner after row lock
PASS: MySQL migration and Mapper regression, assertions=18
```

测试夹具中的系统用户、角色和菜单为明确的测试数据；没有使用生产用户或向实际业务库写入数据。

## 前端与机械闸门

前端执行 `pnpm typecheck`、本次改动 Vue 文件的 ESLint、报告目录和接口文件的 Oxlint，以及 `pnpm build`，退出码均为 0。构建自动重写的无关路由和组件声明已经还原；还原后类型检查仍通过。

真实前端输出：

```text
$ vue-tsc --noEmit --skipLibCheck
$ vite build --mode prod

Build successful. Please see dist directory
```

仓库实际远端存在 `origin/master`，没有 `origin/main`；按规范尝试 `origin/main` 后确认缺少引用，随后明确使用批准的 `1c66288` 作为完整变更基线，没有创建虚假远端引用。

```powershell
python tools/readability_check.py --changed-only 1c66288 --fail-on error
python tools/structure_check.py --changed-only 1c66288
git diff --check
```

真实输出节选：

```text
可读性闸门报告   扫描 48 个文件    error 0   warn 15
页面结构契约闸门：扫描 5 个文件（D:\workspace\enterprise\admin）
未发现问题 ✅
合计：error 0 / warn 0
```

可读性警告为已有前端长行 7 处、已有 `== null` 比较 4 处、已有超长文件 2 处，以及两个已注释说明的可空返回值。未扩展本次范围去拆分历史文件。

数据库规范检查通过专用容器的 MySQL CLI 指向上述隔离库，真实输出节选：

```text
数据库表规范体检：codex_report_regeneration_test（10 张表：隔离 7 / 共享 3）
[warn] 3 条
  sys_role             shared-table-has-tenant  共享表（白名单）带 tenant_id 但不是标识列 —— 与 TenantLineHandlerImpl.SHARED_TABLES 语义冲突
  sys_role_menu        composite-pk-input       复合主键 (role_id, menu_id) —— 实体必须 @TableId(type = IdType.INPUT)
  sys_user_role        composite-pk-input       复合主键 (user_id, role_id) —— 实体必须 @TableId(type = IdType.INPUT)
合计：error 0 / warn 3
```

新增版本登记表无错误。三条警告来自测试夹具采用的现有系统表结构。

前端 premium strict 审计在本次报告页面范围内为 `errors: 0, findings: []`。全前端审计仍有两个已有错误：主题配置文本框 resize、全局滚动条标准属性；本次未修改这两处。

## 独立复审与验收边界

独立只读 reviewer 进行了两轮检查，第二轮结论为通过、无剩余阻塞，并核对 47 项测试和 18 条数据库断言的真实输出。

以下尚未执行，必须按 [验收清单](report-regeneration-acceptance.md) 在验收环境完成：

- 浏览器中用真实角色、业务数据执行完整操作流程与交互检查。
- 真实 SMTP 邮箱、上上签及 OnlyOffice 联合流程。
- 外部 scanner 实际重扫与写库兼容性联调。其批次路径参数按现有源码核实为 `日期/产品/样本编号`，另传配置根目录；本次未改外部 scanner。

批次重扫默认关闭：`DRIVER_BATCH_ENABLED=false`。接口与调用准备已实现，默认点击重新解读将提示未开放，不会创建新报告。联调成功后再开放。
