# 报告重生成与版本流程验收

开发分支：`codex/report-regeneration-flow`，基线 `1c66288`。验收完成前不合并。

## 上线准备

1. 备份数据库和制品目录，执行新增迁移 `template-backend/script/sql/update/update_5.6.22-report-regeneration.sql`。该迁移可重复执行，不回填旧版本、不删除现有测试记录。
2. 前后端一同部署。当前记录不带业务版本时仍可读取旧制品；首次使用新生成流程时才登记业务版本。
3. 配置测试发件邮箱及收件邮箱，再进行邮件全流程验收。不能将单元测试的邮件 mock 当成真实 SMTP 联调。
4. `DRIVER_BATCH_ENABLED` 默认 `false`。外部 scanner 完成当前表结构、租户、analysis_id、Loaded 文件状态及样本级驱动状态兼容后才设为 `true`；本次不改外部驱动项目。

## 业务验收用例

| 用例 | 操作 | 预期 |
|---|---|---|
| 首次生成 | 从生信看点、复核、认领进入解读，选择模板并生成 | 名称为 `client_subbarcode_template_name_V1`，生成 JSON/DOCX/PDF，进入 32 |
| 未送审重生成 | 修改解读参数，查看实时预览，再生成 | 业务版本仍为 V1，内部生成轮次增加，当前文件被替换 |
| 审核中限制 | 在 40、41 状态尝试生成，含直接请求接口 | 服务端拒绝，文件和审核对象不变 |
| 驳回重出 | 审核人驳回，解读人修改并重新生成 | 驳回轮次文件和原因归档，业务版本不递增 |
| 原审核人复审 | 重出后重新提交 | 直接进入 41，原审核人的“报告审核中／我的”可见，其他人无法审核 |
| 审核人失效 | 停用原审核人或撤销审核权限后提交 | 明确拒绝；恢复资格后可提交；产出人不能审核自身报告 |
| 签名失败 | 52 状态选择重新送签或重新生成 | 重送签沿用审核；重生成清除旧合同和签名有效引用，并重新审核 |
| 迟到回调 | 重生成后再提交旧合同的回调 | 旧合同不能更新新一轮状态或签名件 |
| 生成异常 | 制造模板、Python、PDF 转换或登记失败 | 本轮失败为 33，半成品及错误进入 failed；保留上一轮文件，不能送审旧制品 |
| 发送失败重试 | 60 状态发送失败后修复并重试 | 61 可重发，版本不变；成功为 62，消耗该版本 |
| 发送结果不明 | SMTP 成功后数据库登记异常 | 保留待核对日志，阻止直接重发，先核对邮箱与发送日志 |
| 下一版 | 已发送后重新解读，再完成生成 | 新报告关联旧 report_id，旧报告与文件保留；同序列下一版为 V2 |
| 同序列并发 | 同客户、样本、模板的不同批次同时生成 | 仅一份在制报告占号，另一份明确提示冲突 |
| 租户隔离 | 两租户使用相同客户、样本、模板 | 各自从 V1 计数，不能借用另一租户的普通审核人 |
| 重解读未开放 | 默认配置点击“重新解读” | 明确提示尚未开放，不创建新报告、不跳转成功页 |
| 重扫失败 | 开启后返回部分失败、零文件、超时或数据库驱动状态未成功 | 不创建新报告，修复后可重试 |
| 重扫成功重试 | 成功后重复提交同一来源请求 | 返回已经创建的后续记录，不重复扫描或建单；新记录从 20 生信看点开始 |

## 制品目录与恢复

新制品位于配置的 `report.artifact.root` 下：

```text
report/<tenantId>/<reportId>/V<n>/
  current/{json,docx,pdf,signed}/<reportName>.<ext>
  rejected/<generationNo>/{json,docx,pdf,signed}/...
  rejected/<generationNo>/manifest.json
  failed/<generationNo>/...
  failed/<generationNo>/manifest.json
```

名称主体首次生成时按版本序列固化，模板命名配置变化不会重命名该序列；同一业务版本重复生成保持文件名不变；签名件由新合同重新取得，与 PDF 同名、目录不同。发布先在 staging 完成整轮文件，再切换 current；数据库登记失败时恢复旧 current。未能清理的 backup/staging 会保留供故障排查，不能直接批量删除。

回退应用时保留新列、版本登记和已发送制品，避免重新使用已消耗的版本。回退旧程序会恢复旧程序的生成限制与文件规则，须先暂停报告写操作再安排回退。

## 自动验证命令

后端先安装依赖，再从 `template-backend` 执行定向测试，显式关闭测试跳过并选择 local 标签。具体测试名单与真实结果见 [验证记录](report-regeneration-verification.md)。

数据库测试使用专用 MySQL 8 容器 `codex-report-regeneration-mysql`，无端口暴露；凭据从容器环境读取。测试脚本只允许 `codex-report-` 前缀的专用容器，只创建或清理固定的隔离库 `codex_report_regeneration_test`。

本次临时容器在交付时清理。复跑时先确保 Docker Desktop 已运行、本地有 `mysql:8.0.42`，且同名容器不存在；以下命令创建专用容器，使用一次性随机凭据，不写入文件。启动后需等待 MySQL 就绪再运行测试。

```powershell
$env:MYSQL_ROOT_PASSWORD=[guid]::NewGuid().ToString('N')
docker run --detach --name codex-report-regeneration-mysql --network none --tmpfs /var/lib/mysql:rw,size=512m --env MYSQL_ROOT_PASSWORD mysql:8.0.42
Remove-Item Env:MYSQL_ROOT_PASSWORD
docker exec codex-report-regeneration-mysql sh -c 'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; mysqladmin -uroot ping --silent'
```

```powershell
$env:PYTHONIOENCODING='utf-8'
python template-backend/script/tests/test_report_regeneration_db.py --keep-db
python tools/readability_check.py --changed-only 1c66288 --fail-on error
python tools/structure_check.py --changed-only 1c66288
```

预期输出包括 `PASS: MySQL migration and Mapper regression, assertions=18`、可读性 error 0、结构 error 0。数据库规范闸门须指向该隔离库；不能将缺少 Docker 默认容器时的空结果视为通过。

数据库闸门可在同一终端通过 Python 构造 CLI 参数，避免 PowerShell 多层引号影响密码变量的容器内展开：

```powershell
@'
import shlex, subprocess
command = shlex.join(['docker', 'exec', '-i', 'codex-report-regeneration-mysql', 'sh', '-c', 'export MYSQL_PWD="$MYSQL_ROOT_PASSWORD"; exec mysql -uroot -N -B --default-character-set=utf8mb4'])
raise SystemExit(subprocess.call(['python', 'tools/check_db_schema.py', '--database', 'codex_report_regeneration_test', '--mysql-cmd', command]))
'@ | python -
```

完成后仅清理本次专用容器及其临时数据：

```powershell
docker rm -f codex-report-regeneration-mysql
```

## 待外部验收

- 真实 SMTP、上上签、OnlyOffice 与完整业务数据的联合流程需使用验收环境验证。
- scanner 参数格式已通过旁边项目源码核对，但部署脚本的实际写库兼容性仍待联调；默认开关保持关闭。
- 本次自动测试使用明确标注的单元测试 fixture、mock 外部服务以及真实 MySQL 8／子进程，不代表已向客户发送或完成真实电子签署。
